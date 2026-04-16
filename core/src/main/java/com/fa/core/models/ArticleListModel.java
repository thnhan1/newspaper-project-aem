package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.fa.core.services.ArticleQueryService;
import com.fa.core.services.ArticleSummary;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Model(
        adaptables = SlingHttpServletRequest.class,
        resourceType = ArticleListModel.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ArticleListModel {

    public static final String RESOURCE_TYPE = "newspaper/components/content/article-list";

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private PageManager pageManager;

    @ScriptVariable
    private Resource resource;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private ArticleQueryService articleQueryService;

    @ValueMapValue
    @Default(values = "tags")
    private String queryMode; // tags|folder

    @ValueMapValue
    private String listRoot;

    @ValueMapValue
    private String folderRoot;

    @ValueMapValue
    private String[] categories;

    @ValueMapValue
    @Default(intValues = 12)
    private int limit;

    @ValueMapValue
    @Default(values = "desc")
    private String sortOrder; // asc|desc

    @ValueMapValue
    private String featuredArticle;

    @ValueMapValue
    @Default(booleanValues = true)
    private boolean excludeFeatured;

    private List<ArticleCard> items;

    @PostConstruct
    protected void init() {
        if (articleQueryService == null || request == null) {
            items = Collections.emptyList();
            return;
        }

        String rootPath = resolveRootPath();
        if (StringUtils.isBlank(rootPath)) {
            items = Collections.emptyList();
            return;
        }

        ArticleQueryService.QueryMode mode = "folder".equalsIgnoreCase(queryMode)
                ? ArticleQueryService.QueryMode.FOLDER
                : ArticleQueryService.QueryMode.TAGS;

        String effectiveRoot = mode == ArticleQueryService.QueryMode.FOLDER
                ? StringUtils.defaultIfBlank(folderRoot, rootPath)
                : rootPath;

        List<String> tagList = categories != null
                ? java.util.Arrays.stream(categories).filter(StringUtils::isNotBlank).collect(Collectors.toList())
                : Collections.emptyList();

        ArticleQueryService.QueryParams params = new ArticleQueryService.QueryParams(
                mode,
                effectiveRoot,
                tagList,
                limit,
                "asc".equalsIgnoreCase(sortOrder),
                featuredArticle,
                excludeFeatured
        );

        List<ArticleSummary> summaries = articleQueryService.getArticles(request.getResourceResolver(), params);
        items = summaries.stream().map(ArticleCard::from).collect(Collectors.toList());
    }

    public String getId() {
        return "cmp-article-list-" + Math.abs((resource != null ? resource.getPath() : "").hashCode());
    }

    public List<ArticleCard> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    public String getListRoot() {
        return StringUtils.defaultIfBlank(listRoot, "");
    }

    private String resolveRootPath() {
        if (pageManager == null) {
            return null;
        }
        if (StringUtils.isNotBlank(listRoot)) {
            Page p = pageManager.getPage(listRoot);
            if (p != null) {
                return p.getPath();
            }
        }
        return currentPage != null ? currentPage.getPath() : null;
    }

    public static final class ArticleCard {
        private final String title;
        private final String url;
        private final String description;
        private final java.time.Instant date;
        private final String image;
        private final String imageAlt;

        private ArticleCard(String title,
                            String url,
                            String description,
                            java.time.Instant date,
                            String image,
                            String imageAlt) {
            this.title = title;
            this.url = url;
            this.description = description;
            this.date = date;
            this.image = image;
            this.imageAlt = imageAlt;
        }

        static ArticleCard from(ArticleSummary s) {
            return new ArticleCard(
                    s.getTitle(),
                    s.getUrl(),
                    s.getDescription(),
                    s.getDate(),
                    StringUtils.defaultString(s.getImage()),
                    StringUtils.defaultString(s.getImageAlt())
            );
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getDescription() {
            return description;
        }

        public java.time.Instant getDate() {
            return date;
        }

        public String getImage() {
            return image;
        }

        public String getImageAlt() {
            return imageAlt;
        }
    }
}

