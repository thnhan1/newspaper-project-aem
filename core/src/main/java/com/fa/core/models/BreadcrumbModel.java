package com.fa.core.models;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.commons.link.LinkManager;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = Breadcrumb.class,
        resourceType = BreadcrumbModel.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class BreadcrumbModel implements Breadcrumb {

    private static final Logger LOG = LoggerFactory.getLogger(BreadcrumbModel.class);

    public static final String RESOURCE_TYPE = "newspaper/components/structure/breadcrumb";

    private static final String TAG_NAMESPACE = "newspaper:";
    private static final String ARTICLES_SEGMENT = "/articles/";
    private static final int LANGUAGE_ROOT_DEPTH = 3; // /content/newspaper/language-masters/en

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private PageManager pageManager;

    @ScriptVariable
    private Resource resource;

    @Self
    private LinkManager linkManager;

    @ValueMapValue
    @Default(booleanValues = false)
    private boolean hideCurrent;

    @ValueMapValue
    @Default(booleanValues = false)
    private boolean showHidden;

    @ValueMapValue
    @Default(intValues = 2)
    private int startLevel;

    @ValueMapValue
    @Default(booleanValues = true)
    private boolean disableShadowing;

    @ValueMapValue
    private String selectedTag;

    private List<NavigationItem> items;

    @PostConstruct
    protected void init() {
        items = new ArrayList<>();

        if (currentPage == null) {
            return;
        }

        if (!currentPage.getPath().contains(ARTICLES_SEGMENT)) {
            return;
        }

        buildBreadcrumbItems();
    }

    @Override
    public Collection<NavigationItem> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    private void buildBreadcrumbItems() {
        // Get language root (e.g., /content/newspaper/language-masters/en)
        Page languageRoot = currentPage.getAbsoluteParent(LANGUAGE_ROOT_DEPTH);
        if (languageRoot == null) {
            LOG.warn("Could not find language root at depth {} for page: {}", LANGUAGE_ROOT_DEPTH, currentPage.getPath());
            fallbackToSimpleBreadcrumb();
            return;
        }

        LOG.info("Language root found: {}", languageRoot.getPath());
        
        // Add Home (language root) as first item
        items.add(new BreadcrumbItem(languageRoot, false, linkManager, true));

        String tagPath = extractTagPath();

        if (StringUtils.isNotBlank(tagPath)) {
            LOG.info("Building breadcrumb from tag path: {}", tagPath);
            String[] segments = tagPath.split("/");
            
            // Build path from language root
            // MSM: All languages use same structure (news/tech)
            String basePath = languageRoot.getPath();

            for (String segment : segments) {
                String topicPath = basePath + "/" + segment;
                LOG.debug("Looking for topic/subtopic page at path: {}", topicPath);
                Page topicPage = pageManager.getPage(topicPath);

                if (topicPage != null) {
                    LOG.info("Found topic page: {} (title: {}, hideInNav: {})", 
                             topicPage.getPath(), topicPage.getTitle(), topicPage.isHideInNav());
                    if (showHidden || !topicPage.isHideInNav()) {
                        items.add(new BreadcrumbItem(topicPage, false, linkManager, false));
                        LOG.debug("Added breadcrumb item: {}", topicPage.getTitle());
                    } else {
                        LOG.debug("Skipping hidden page: {}", topicPage.getPath());
                    }
                } else {
                    LOG.warn("Topic/subtopic page not found at path: {} - skipping this segment", topicPath);
                }
                
                // Update base path for nested segments (e.g., news -> news/tech)
                basePath = topicPath;
            }
        } else {
            LOG.info("No tag path found, using simple breadcrumb for page: {}", currentPage.getPath());
        }

        // Add current article page
        if (!hideCurrent) {
            items.add(new BreadcrumbItem(currentPage, true, linkManager, false));
            LOG.debug("Added current page to breadcrumb: {}", currentPage.getTitle());
        }
        
        LOG.info("Built breadcrumb with {} items for page: {}", items.size(), currentPage.getPath());
    }

    private void fallbackToSimpleBreadcrumb() {
        Page languageRoot = currentPage.getAbsoluteParent(LANGUAGE_ROOT_DEPTH);
        if (languageRoot != null) {
            items.add(new BreadcrumbItem(languageRoot, false, linkManager, true));
            LOG.debug("Fallback: Added language root as home: {}", languageRoot.getPath());
        }
        if (!hideCurrent && currentPage != null) {
            items.add(new BreadcrumbItem(currentPage, true, linkManager, false));
            LOG.debug("Fallback: Added current page: {}", currentPage.getPath());
        }
    }

    private String extractTagPath() {
        LOG.debug("Extracting tag path for page: {}", currentPage.getPath());
        
        // Check if author selected a specific tag in dialog
        if (StringUtils.isNotBlank(selectedTag)) {
            LOG.debug("Using selected tag from dialog: {}", selectedTag);
            if (selectedTag.startsWith(TAG_NAMESPACE)) {
                String tagPath = selectedTag.substring(TAG_NAMESPACE.length());
                LOG.debug("Extracted tag path from selected tag: {}", tagPath);
                return tagPath;
            }
            LOG.warn("Selected tag does not start with namespace {}: {}", TAG_NAMESPACE, selectedTag);
            return null;
        }

        // Get tags from page content resource (jcr:content)
        Resource contentResource = currentPage.getContentResource();
        if (contentResource == null) {
            LOG.warn("No content resource found for page: {}", currentPage.getPath());
            return null;
        }

        ValueMap pageProperties = contentResource.getValueMap();
        String[] tags = pageProperties.get("cq:tags", String[].class);

        LOG.debug("Found tags on page {}: {}", currentPage.getPath(), 
                  tags != null ? Arrays.toString(tags) : "null");

        if (tags == null || tags.length == 0) {
            LOG.debug("No tags found on page: {}", currentPage.getPath());
            return null;
        }

        for (String tag : tags) {
            LOG.debug("Checking tag: {}", tag);
            if (tag.startsWith(TAG_NAMESPACE)) {
                String tagPath = tag.substring(TAG_NAMESPACE.length());
                LOG.info("Using tag '{}' for breadcrumb, extracted path: {}", tag, tagPath);
                return tagPath;
            }
        }

        LOG.debug("No newspaper namespace tags found in: {}", Arrays.toString(tags));
        return null;
    }

    @Override
    public String getId() {
        String id = resource.getValueMap().get("id", String.class);
        return StringUtils.isNotBlank(id) ? id : "cmp-breadcrumb-" + Math.abs(resource.getPath().hashCode());
    }

    private static class BreadcrumbItem implements NavigationItem {

        private final Page page;
        private final boolean active;
        private final Link<Page> link;
        private final boolean isRoot;

        @SuppressWarnings("unchecked")
        BreadcrumbItem(Page page, boolean active, LinkManager linkManager, boolean isRoot) {
            this.page = page;
            this.active = active;
            this.isRoot = isRoot;
            this.link = linkManager.get(page).build();
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
            if (isRoot) {
                return "Home";
            }

            if (StringUtils.isNotBlank(page.getNavigationTitle())) {
                return page.getNavigationTitle();
            }

            if (StringUtils.isNotBlank(page.getPageTitle())) {
                return page.getPageTitle();
            }

            if (StringUtils.isNotBlank(page.getTitle())) {
                return page.getTitle();
            }

            return page.getName();
        }

        @Override
        public String getPath() {
            return page.getPath();
        }

        @Override
        public String getURL() {
            return link.getURL();
        }

        @Override
        public Link<Page> getLink() {
            return link;
        }

        @Override
        public Resource getTeaserResource() {
            return null;
        }
    }
}