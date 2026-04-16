package com.fa.core.models;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Navigation;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
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

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = Navigation.class,
        resourceType = NavModel.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class NavModel implements Navigation {

    private static final Logger LOG = LoggerFactory.getLogger(NavModel.class);

    protected static final String RESOURCE_TYPE = "newspaper/components/structure/nav";

    private static final String TAG_NAMESPACE = "newspaper:";
    private static final String PN_CQ_TAGS = "cq:tags";

    @Self
    @Via(type = ResourceSuperType.class)
    private Navigation delegate;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private PageManager pageManager;

    @ScriptVariable
    private Resource resource;

    private List<NavigationItem> items;

    private TagContext tagContext;

    @PostConstruct
    protected void init() {
        tagContext = TagContext.from(currentPage, pageManager);

        if (delegate != null) {
            Collection<NavigationItem> delegateItems = delegate.getItems();
            if (delegateItems != null && !delegateItems.isEmpty()) {
                items = new ArrayList<>();
                for (NavigationItem item : delegateItems) {
                    items.add(new EnhancedItem(item, tagContext));
                }
            }
        }
    }

    @Override
    public List<NavigationItem> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    @Override
    public String getAccessibilityLabel() {
        return delegate != null ? delegate.getAccessibilityLabel() : "Main Navigation";
    }

    @Override
    public String getId() {
        return delegate != null ? delegate.getId()
                : "nav-" + Math.abs(resource.getPath().hashCode());
    }

    private static class EnhancedItem implements NavigationItem {

        private final NavigationItem delegate;
        private final TagContext tagContext;
        private List<NavigationItem> children;

        EnhancedItem(NavigationItem delegate, TagContext tagContext) {
            this.delegate = delegate;
            this.tagContext = tagContext;
        }

        @Override
        public Page getPage() {
            Link<Page> link = getLink();
            return link != null ? link.getReference() : null;
        }

        @Override
        public boolean isActive() {
            if (delegate.isActive()) {
                return true;
            }
            return tagContext != null && tagContext.matches(getPath());
        }

        @Override
        public boolean isCurrent() {
            if (delegate.isCurrent()) {
                return true;
            }
            return tagContext != null && tagContext.matches(getPath());
        }

        @Override
        @SuppressWarnings("unchecked")
        public Link<Page> getLink() {
            return delegate.getLink();
        }

        @Override
        public String getURL() {
            Link<Page> link = getLink();
            return link != null ? link.getURL() : "";
        }

        @Override
        public String getPath() {
            return delegate.getPath();
        }

        @Override
        public String getTitle() {
            return delegate.getTitle();
        }

        @Override
        public int getLevel() {
            return delegate.getLevel();
        }

        @Override
        public List<NavigationItem> getChildren() {
            if (children == null) {
                children = loadChildren();
            }
            return children;
        }

        @Override
        public Resource getTeaserResource() {
            return delegate.getTeaserResource();
        }

        private List<NavigationItem> loadChildren() {
            Link<Page> link = getLink();
            Page page = link != null ? link.getReference() : null;
            if (page == null) {
                return Collections.emptyList();
            }
            List<NavigationItem> result = new ArrayList<>();
            Iterator<Page> childPages = page.listChildren(new PageFilter(false, true));
            while (childPages.hasNext()) {
                Page child = childPages.next();
                if (!child.isHideInNav()) {
                    result.add(new ChildItem(child, tagContext));
                }
            }
            return result;
        }
    }

    private static class ChildItem implements NavigationItem {

        private final Page page;
        private final TagContext tagContext;

        ChildItem(Page page, TagContext tagContext) {
            this.page = page;
            this.tagContext = tagContext;
        }

        @Override
        public Page getPage() {
            return page;
        }

        @Override
        public boolean isActive() {
            return tagContext != null && tagContext.matches(page.getPath());
        }

        @Override
        public boolean isCurrent() {
            return tagContext != null && tagContext.matches(page.getPath());
        }

        @Override
        public Link<Page> getLink() {
            return null;
        }

        @Override
        public String getURL() {
            return page.getPath() + ".html";
        }

        @Override
        public String getPath() {
            return page.getPath();
        }

        @Override
        public String getTitle() {
            String navTitle = page.getNavigationTitle();
            if (StringUtils.isNotBlank(navTitle)) {
                return navTitle;
            }
            String title = page.getTitle();
            return StringUtils.isNotBlank(title) ? title : page.getName();
        }

        @Override
        public int getLevel() {
            return page.getDepth();
        }

        @Override
        public List<NavigationItem> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public Resource getTeaserResource() {
            return null;
        }
    }

    /**
     * Resolves the primary tag from the article page, strips the namespace,
     * then resolves page paths from the tag segments under the language root.
     * Used to highlight the correct nav items when viewing an article.
     *
     * Example: tag newspaper:news/technology on language root .../en
     * → activePaths = {.../en/news, .../en/news/technology}
     */
    static final class TagContext {
        private final Set<String> activePaths;

        private TagContext(Set<String> activePaths) {
            this.activePaths = activePaths;
        }

        static TagContext from(Page currentPage, PageManager pageManager) {
            if (currentPage == null || pageManager == null) {
                return null;
            }

            String primaryTag = resolveFirstNewspaperTag(currentPage);
            if (primaryTag == null) {
                return null;
            }

            Page languageRoot = BreadcrumbModel.findLanguageRoot(currentPage);
            if (languageRoot == null) {
                return null;
            }

            String tagPath = primaryTag;
            if (tagPath.startsWith(TAG_NAMESPACE)) {
                tagPath = tagPath.substring(TAG_NAMESPACE.length());
            }
            if (StringUtils.isBlank(tagPath)) {
                return null;
            }

            String[] segments = tagPath.split("/");
            Set<String> paths = new HashSet<>();
            String currentPath = languageRoot.getPath();

            for (String segment : segments) {
                currentPath = currentPath + "/" + segment;
                Page page = pageManager.getPage(currentPath);
                if (page != null) {
                    paths.add(page.getPath());
                }
            }

            return paths.isEmpty() ? null : new TagContext(paths);
        }

        boolean matches(String path) {
            return activePaths != null && activePaths.contains(path);
        }

        private static String resolveFirstNewspaperTag(Page page) {
            Resource contentResource = page.getContentResource();
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
    }
}
