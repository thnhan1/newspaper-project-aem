package com.fa.core.services.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;
import com.fa.core.services.ArticleQueryService;
import com.fa.core.services.ArticleSummary;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component(service = ArticleQueryService.class)
@Designate(ocd = ArticleQueryServiceImpl.Config.class)
public class ArticleQueryServiceImpl implements ArticleQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleQueryServiceImpl.class);

    private static final String ARTICLES_SEGMENT = "/articles/";

    @ObjectClassDefinition(
            name = "Newspaper Article Query Service",
            description = "Queries article pages by cq:tags or folder with caching"
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Cache TTL (seconds)",
                description = "How long to cache query results. Default 3600s = 1 hour."
        )
        int cache_ttl_seconds() default 3600;

        @AttributeDefinition(
                name = "Cache max entries",
                description = "Maximum number of cached query keys to keep in memory."
        )
        int cache_max_entries() default 200;
    }

    private volatile int ttlSeconds;
    private volatile int maxEntries;

    private final Map<String, CacheEntry> cache = Collections.synchronizedMap(new LinkedHashMap<String, CacheEntry>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > maxEntries;
        }
    });

    @Activate
    protected void activate(Config config) {
        ttlSeconds = Math.max(1, config.cache_ttl_seconds());
        maxEntries = Math.max(10, config.cache_max_entries());
        LOG.info("ArticleQueryService activated. ttlSeconds={}, maxEntries={}", ttlSeconds, maxEntries);
    }

    @Override
    public List<ArticleSummary> getArticles(ResourceResolver resourceResolver, QueryParams params) {
        if (resourceResolver == null || params == null) {
            return Collections.emptyList();
        }

        String root = StringUtils.defaultIfBlank(params.getRootPath(), "/content");
        int limit = Math.max(0, params.getLimit());

        String cacheKey = buildCacheKey(params);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(ttlSeconds)) {
            return cached.copyLimited(limit);
        }

        List<ArticleSummary> results;
        if (params.getMode() == QueryMode.FOLDER) {
            results = queryByFolder(resourceResolver, root, limit, params.isSortAscending());
        } else {
            results = queryByTags(resourceResolver, root, params.getCategoryTags(), limit, params.isSortAscending());
        }

        // Featured handling
        results = applyFeatured(resourceResolver, params, results);

        cache.put(cacheKey, new CacheEntry(results));
        return limitResults(results, limit);
    }

    private static String buildCacheKey(QueryParams params) {
        return String.join("|",
                "mode=" + (params.getMode() != null ? params.getMode().name() : "TAGS"),
                "root=" + StringUtils.defaultString(params.getRootPath()),
                "tags=" + (params.getCategoryTags() != null ? String.join(",", params.getCategoryTags()) : ""),
                "limit=" + params.getLimit(),
                "sortAsc=" + params.isSortAscending(),
                "featured=" + StringUtils.defaultString(params.getFeaturedArticlePath()),
                "excludeFeatured=" + params.isExcludeFeaturedFromList()
        );
    }

    private static List<ArticleSummary> queryByFolder(ResourceResolver rr, String rootPath, int limit, boolean sortAsc) {
        PageManager pm = rr.adaptTo(PageManager.class);
        if (pm == null) {
            return Collections.emptyList();
        }
        Page root = pm.getPage(rootPath);
        if (root == null) {
            return Collections.emptyList();
        }

        List<ArticleSummary> items = new ArrayList<>();
        Iterator<Page> it = root.listChildren(new PageFilter(false, true), true);
        while (it.hasNext()) {
            Page p = it.next();
            if (p == null || p.isHideInNav()) {
                continue;
            }
            if (!p.getPath().contains(ARTICLES_SEGMENT)) {
                continue;
            }
            items.add(toSummary(p));
        }

        sort(items, sortAsc);
        return limitResults(items, limit);
    }

    private static List<ArticleSummary> queryByTags(ResourceResolver rr, String rootPath, List<String> tags, int limit, boolean sortAsc) {
        QueryBuilder qb = rr.adaptTo(QueryBuilder.class);
        Session session = rr.adaptTo(Session.class);
        if (qb == null || session == null) {
            return Collections.emptyList();
        }

        Map<String, String> predicates = new LinkedHashMap<>();
        predicates.put("path", rootPath);
        predicates.put("type", "cq:Page");
        predicates.put("p.limit", String.valueOf(Math.max(limit, 50))); // fetch enough for sorting
        predicates.put("1_property", "jcr:content/cq:tags");

        if (tags != null && !tags.isEmpty()) {
            predicates.put("taggroup.p.or", "true");
            int i = 1;
            for (String tag : tags) {
                if (StringUtils.isBlank(tag)) {
                    continue;
                }
                predicates.put("taggroup." + i + "_property", "jcr:content/cq:tags");
                predicates.put("taggroup." + i + "_property.value", tag);
                i++;
            }
            if (i == 1) {
                // no usable tags
                predicates.remove("taggroup.p.or");
            }
        }

        // Sort by last modified (same as typical newsroom)
        predicates.put("orderby", "@jcr:content/cq:lastModified");
        predicates.put("orderby.sort", sortAsc ? "asc" : "desc");

        Query query = qb.createQuery(PredicateGroup.create(predicates), session);
        SearchResult sr = query.getResult();

        List<ArticleSummary> items = new ArrayList<>();
        for (Hit hit : sr.getHits()) {
            try {
                Resource res = hit.getResource();
                Page page = res != null ? res.adaptTo(Page.class) : null;
                if (page == null) {
                    // hit might be page node; try resolve by path
                    String path = hit.getPath();
                    PageManager pm = rr.adaptTo(PageManager.class);
                    page = pm != null ? pm.getPage(path) : null;
                }
                if (page == null) {
                    continue;
                }
                if (!page.getPath().contains(ARTICLES_SEGMENT)) {
                    continue;
                }
                if (page.isHideInNav()) {
                    continue;
                }
                items.add(toSummary(page));
            } catch (Exception e) {
                LOG.debug("Skipping query hit due to error", e);
            }
        }

        // QueryBuilder already sorted but keep deterministic
        sort(items, sortAsc);
        return limitResults(items, limit);
    }

    private static void sort(List<ArticleSummary> items, boolean asc) {
        Comparator<ArticleSummary> cmp = Comparator.comparing(a -> a.getDate() != null ? a.getDate() : Instant.EPOCH);
        items.sort(asc ? cmp : cmp.reversed());
    }

    private static ArticleSummary toSummary(Page page) {
        String title = StringUtils.firstNonBlank(page.getPageTitle(), page.getTitle(), page.getName());
        String url = page.getPath() + ".html";
        String desc = StringUtils.defaultIfBlank(page.getDescription(), "");
        Instant date = null;
        Object lastModified = page.getProperties().get("cq:lastModified");
        if (lastModified instanceof java.util.Calendar) {
            date = ((java.util.Calendar) lastModified).toInstant();
        }

        ImagePick img = pickImage(page);
        return new ArticleSummary(title, url, desc, date, page.getPath(), img.src, img.alt);
    }

    private static final class ImagePick {
        final String src;
        final String alt;
        ImagePick(String src, String alt) {
            this.src = src;
            this.alt = alt;
        }
    }

    private static ImagePick pickImage(Page page) {
        if (page == null) {
            return new ImagePick("", "");
        }
        Resource content = page.getContentResource();
        if (content == null) {
            return new ImagePick("", "");
        }

        // Try common properties on jcr:content
        ValueMap vm = content.getValueMap();
        String src = firstNonBlank(
                vm.get("image", String.class),
                vm.get("fileReference", String.class),
                vm.get("featuredImage", String.class),
                vm.get("thumbnail", String.class)
        );
        String alt = firstNonBlank(
                vm.get("imageAlt", String.class),
                vm.get("alt", String.class),
                page.getTitle()
        );

        // Try common child nodes
        if (StringUtils.isBlank(src)) {
            src = readChildImage(content, "image");
        }
        if (StringUtils.isBlank(src)) {
            src = readChildImage(content, "featuredImage");
        }
        if (StringUtils.isBlank(src)) {
            src = readChildImage(content, "thumbnail");
        }

        return new ImagePick(StringUtils.defaultString(src), StringUtils.defaultString(alt));
    }

    private static String readChildImage(Resource content, String childName) {
        Resource child = content.getChild(childName);
        if (child == null) {
            return null;
        }
        ValueMap vm = child.getValueMap();
        return firstNonBlank(
                vm.get("fileReference", String.class),
                vm.get("image", String.class),
                vm.get("src", String.class)
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return v;
            }
        }
        return null;
    }

    private static List<ArticleSummary> limitResults(List<ArticleSummary> items, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        if (items.size() <= limit) {
            return items;
        }
        return new ArrayList<>(items.subList(0, limit));
    }

    private static List<ArticleSummary> applyFeatured(ResourceResolver rr, QueryParams params, List<ArticleSummary> base) {
        if (params == null || base == null) {
            return base;
        }

        String featuredPath = StringUtils.trimToNull(params.getFeaturedArticlePath());
        if (featuredPath == null) {
            return base;
        }

        PageManager pm = rr.adaptTo(PageManager.class);
        if (pm == null) {
            return base;
        }
        Page featured = pm.getPage(featuredPath);
        if (featured == null) {
            return base;
        }

        ArticleSummary featuredSummary = toSummary(featured);
        List<ArticleSummary> result = new ArrayList<>();
        result.add(featuredSummary);

        for (ArticleSummary s : base) {
            if (params.isExcludeFeaturedFromList() && StringUtils.equals(s.getPath(), featuredPath)) {
                continue;
            }
            result.add(s);
        }

        return result;
    }

    private static final class CacheEntry {
        private final long createdMs;
        private final List<ArticleSummary> items;

        CacheEntry(List<ArticleSummary> items) {
            this.createdMs = System.currentTimeMillis();
            this.items = items != null ? new ArrayList<>(items) : Collections.emptyList();
        }

        boolean isExpired(int ttlSeconds) {
            return System.currentTimeMillis() - createdMs > (ttlSeconds * 1000L);
        }

        List<ArticleSummary> copyLimited(int limit) {
            return limitResults(new ArrayList<>(items), limit);
        }
    }
}

