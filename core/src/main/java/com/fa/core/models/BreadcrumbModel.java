package com.fa.core.models;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sling Model mở rộng Core Breadcrumb v3.
 *
 * <ul>
 *   <li>Taxonomy page: delegate hoàn toàn cho Core Component.</li>
 *   <li>Article page (path chứa "/articles/"): đọc {@code cq:tags},
 *       map tag ID thành page path (newspaper:news/world → {root}/news/world),
 *       build: Home > News > World > Article title.</li>
 * </ul>
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = Breadcrumb.class,
        resourceType = BreadcrumbModel.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class BreadcrumbModel implements Breadcrumb {

    private static final Logger LOG = LoggerFactory.getLogger(BreadcrumbModel.class);

    static final String RESOURCE_TYPE = "newspaper/components/structure/breadcrumb";
    private static final String ARTICLES_SEGMENT = "/articles/";
    private static final String TAG_NAMESPACE = "newspaper:";
    private static final int DEFAULT_ROOT_DEPTH = 4;

    @Self
    @Via(type = ResourceSuperType.class)
    private Breadcrumb delegate;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private PageManager pageManager;

    @Override
    public Collection<NavigationItem> getItems() {
        if (currentPage == null) {
            return delegateItems();
        }
        if (!currentPage.getPath().contains(ARTICLES_SEGMENT)) {
            return delegateItems();
        }
        return buildArticleBreadcrumb();
    }

    @Override
    public String getId() {
        return delegate != null ? delegate.getId() : null;
    }

    private Collection<NavigationItem> delegateItems() {
        return delegate != null ? delegate.getItems() : Collections.emptyList();
    }

    private Collection<NavigationItem> buildArticleBreadcrumb() {
        Page rootPage = currentPage.getAbsoluteParent(DEFAULT_ROOT_DEPTH);
        if (rootPage == null || pageManager == null) {
            return delegateItems();
        }

        Page taxonomyPage = resolveTagToPage(rootPage);

        List<NavigationItem> result = new ArrayList<>();

        result.add(new SimpleBreadcrumbItem(rootPage, false));

        if (taxonomyPage != null) {
            Deque<Page> trail = new ArrayDeque<>();
            Page cursor = taxonomyPage;
            while (cursor != null
                    && cursor.getDepth() > rootPage.getDepth()
                    && cursor.getPath().startsWith(rootPage.getPath())) {
                trail.push(cursor);
                cursor = cursor.getParent();
            }
            for (Page page : trail) {
                result.add(new SimpleBreadcrumbItem(page, false));
            }
        }

        result.add(new SimpleBreadcrumbItem(currentPage, true));

        return Collections.unmodifiableList(result);
    }

    /**
     * Map cq:tags trực tiếp thành page path.
     * Tag "newspaper:news/world" → strip prefix → "news/world" → rootPath + "/news/world".
     * Ưu tiên tag có path sâu nhất (cụ thể nhất).
     */
    private Page resolveTagToPage(Page rootPage) {
        String[] tags = currentPage.getProperties().get("cq:tags", String[].class);
        if (tags == null || tags.length == 0) {
            return null;
        }

        String rootPath = rootPage.getPath();
        Page bestMatch = null;
        int bestDepth = -1;

        for (String tag : tags) {
            if (!tag.startsWith(TAG_NAMESPACE)) {
                continue;
            }
            String relativePath = tag.substring(TAG_NAMESPACE.length());
            if (StringUtils.isBlank(relativePath)) {
                continue;
            }
            Page page = pageManager.getPage(rootPath + "/" + relativePath);
            if (page != null && page.getDepth() > bestDepth) {
                bestMatch = page;
                bestDepth = page.getDepth();
            }
        }

        return bestMatch;
    }

    private static class SimpleBreadcrumbItem implements NavigationItem {

        private final Page page;
        private final boolean active;
        private final Link<Page> link;

        SimpleBreadcrumbItem(Page page, boolean active) {
            this.page = page;
            this.active = active;
            this.link = active ? null : new SimpleLink(page);
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public boolean isCurrent() {
            return active;
        }

        @Override
        public Page getPage() {
            return page;
        }

        @Override
        public String getTitle() {
            String nav = page.getNavigationTitle();
            if (StringUtils.isNotBlank(nav)) return nav;
            String title = page.getTitle();
            if (StringUtils.isNotBlank(title)) return title;
            return page.getName();
        }

        @Override
        public String getPath() {
            return page.getPath();
        }

        @Override
        public String getURL() {
            return page.getPath() + ".html";
        }

        @Override
        public Link getLink() {
            return link;
        }

        @Override
        public Resource getTeaserResource() {
            return null;
        }
    }

    private static class SimpleLink implements Link<Page> {

        private final Page page;
        private final String url;

        SimpleLink(Page page) {
            this.page = page;
            this.url = page.getPath() + ".html";
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String getURL() {
            return url;
        }

        @Override
        public Map<String, String> getHtmlAttributes() {
            Map<String, String> attrs = new HashMap<>();
            attrs.put("href", url);
            return attrs;
        }

        @Override
        public Page getReference() {
            return page;
        }
    }
}
