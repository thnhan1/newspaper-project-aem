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
    private static final String PN_CQ_TAGS = "cq:tags";

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
    @Default(booleanValues = true)
    private boolean hideCurrent;

    @ValueMapValue
    @Default(booleanValues = false)
    private boolean showHidden;

    @ValueMapValue
    private String primaryTag;

    private List<NavigationItem> items;

    @PostConstruct
    protected void init() {
        items = new ArrayList<>();
        if (currentPage == null) {
            return;
        }

        Page languageRoot = findLanguageRoot(currentPage);
        if (languageRoot == null) {
            LOG.warn("No language root found for page: {}", currentPage.getPath());
            return;
        }

        items.add(new BreadcrumbItem(languageRoot, false, linkManager, true));

        String tagId = resolvePrimaryTag();
        if (StringUtils.isNotBlank(tagId)) {
            buildItemsFromTag(tagId, languageRoot);
        }

        if (!hideCurrent) {
            items.add(new BreadcrumbItem(currentPage, true, linkManager, false));
        }

        LOG.debug("Breadcrumb built with {} items for {}", items.size(), currentPage.getPath());
    }

    @Override
    public Collection<NavigationItem> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    @Override
    public String getId() {
        String id = resource.getValueMap().get("id", String.class);
        return StringUtils.isNotBlank(id) ? id : "cmp-breadcrumb-" + Math.abs(resource.getPath().hashCode());
    }

    /**
     * Finds the language root by walking up the page tree and looking for
     * the cq:language property. Works for both language-masters/en (depth 3)
     * and regional sites like vn/vi (depth 2).
     */
    static Page findLanguageRoot(Page page) {
        Page current = page;
        while (current != null) {
            Resource contentRes = current.getContentResource();
            if (contentRes != null) {
                String lang = contentRes.getValueMap().get("cq:language", String.class);
                if (StringUtils.isNotBlank(lang)) {
                    return current;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Resolves the primary tag to use. Priority:
     * 1. primaryTag selected in component dialog
     * 2. First newspaper: tag found on the current page's cq:tags
     */
    private String resolvePrimaryTag() {
        if (StringUtils.isNotBlank(primaryTag)) {
            return primaryTag;
        }
        Resource contentResource = currentPage.getContentResource();
        if (contentResource == null) {
            return null;
        }
        String[] tags = contentResource.getValueMap().get(PN_CQ_TAGS, String[].class);
        if (tags == null) {
            return null;
        }
        for (String tag : tags) {
            if (tag.startsWith(TAG_NAMESPACE)) {
                return tag;
            }
        }
        return null;
    }

    /**
     * Builds breadcrumb items from primary tag using path-based resolution.
     *
     * Tag format: newspaper:news/technology
     * Strip namespace → news/technology
     * Resolve pages: {langRoot}/news, then {langRoot}/news/technology
     *
     * Each segment in the tag path maps to a child page under the language root.
     */
    private void buildItemsFromTag(String tagId, Page languageRoot) {
        String tagPath = tagId;
        if (tagPath.startsWith(TAG_NAMESPACE)) {
            tagPath = tagPath.substring(TAG_NAMESPACE.length());
        }

        if (StringUtils.isBlank(tagPath)) {
            return;
        }

        String[] segments = tagPath.split("/");
        String currentPath = languageRoot.getPath();

        for (String segment : segments) {
            currentPath = currentPath + "/" + segment;
            Page page = pageManager.getPage(currentPath);
            if (page == null) {
                LOG.debug("Page not found at {}, stopping breadcrumb build", currentPath);
                break;
            }
            if (!showHidden && page.isHideInNav()) {
                LOG.debug("Skipping hidden page: {}", currentPath);
                continue;
            }
            items.add(new BreadcrumbItem(page, false, linkManager, false));
        }
    }

    static class BreadcrumbItem implements NavigationItem {

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
