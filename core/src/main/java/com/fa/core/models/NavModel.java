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
import org.apache.sling.api.resource.ValueMap;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
    private static final String ARTICLES_SEGMENT = "/articles/";
    private static final int LANGUAGE_ROOT_DEPTH = 3; // /content/newspaper/language-masters/en

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
            return tagContext != null && tagContext.matchesTopic(getPath());
        }

        @Override
        public boolean isCurrent() {
            if (delegate.isCurrent()) {
                return true;
            }
            return tagContext != null && tagContext.matchesTopic(getPath());
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
            LOG.debug("Loaded {} children for {}", result.size(), page.getPath());
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
            return tagContext != null && tagContext.matchesSubTopic(page.getPath());
        }

        @Override
        public boolean isCurrent() {
            return tagContext != null && tagContext.matchesSubTopic(page.getPath());
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
     * Determines which topic/sub-topic should be active for article pages,
     * using the same tag strategy as BreadcrumbModel (read cq:tags from jcr:content).
     */
    private static final class TagContext {
        private final String topicPath;
        private final String subTopicPath;

        private TagContext(String topicPath, String subTopicPath) {
            this.topicPath = topicPath;
            this.subTopicPath = subTopicPath;
        }

        static TagContext from(Page currentPage, PageManager pageManager) {
            if (currentPage == null || pageManager == null) {
                return null;
            }

            // Only apply tag-derived active state for articles (MSM pattern)
            if (!currentPage.getPath().contains(ARTICLES_SEGMENT)) {
                return null;
            }

            Page languageRoot = currentPage.getAbsoluteParent(LANGUAGE_ROOT_DEPTH);
            if (languageRoot == null) {
                return null;
            }

            String tagPath = extractTagPath(currentPage);
            if (StringUtils.isBlank(tagPath)) {
                return null;
            }

            String[] segments = tagPath.split("/");
            if (segments.length == 0) {
                return null;
            }

            String base = languageRoot.getPath();
            String topicCandidate = base + "/" + segments[0];
            String subCandidate = segments.length >= 2 ? (topicCandidate + "/" + segments[1]) : null;

            // Only accept if pages exist (prevents incorrect highlighting)
            Page topicPage = pageManager.getPage(topicCandidate);
            if (topicPage == null) {
                return null;
            }

            String topicPath = topicPage.getPath();
            String subTopicPath = null;
            if (StringUtils.isNotBlank(subCandidate)) {
                Page subPage = pageManager.getPage(subCandidate);
                if (subPage != null) {
                    subTopicPath = subPage.getPath();
                }
            }

            return new TagContext(topicPath, subTopicPath);
        }

        boolean matchesTopic(String path) {
            return StringUtils.isNotBlank(topicPath) && StringUtils.equals(topicPath, path);
        }

        boolean matchesSubTopic(String path) {
            return StringUtils.isNotBlank(subTopicPath) && StringUtils.equals(subTopicPath, path);
        }

        private static String extractTagPath(Page page) {
            Resource contentResource = page.getContentResource();
            if (contentResource == null) {
                return null;
            }

            ValueMap vm = contentResource.getValueMap();
            String[] tags = vm.get("cq:tags", String[].class);
            if (tags == null || tags.length == 0) {
                return null;
            }

            for (String tag : tags) {
                if (StringUtils.startsWith(tag, TAG_NAMESPACE)) {
                    return tag.substring(TAG_NAMESPACE.length());
                }
            }

            LOG.debug("No {} tags found for {}. Tags: {}", TAG_NAMESPACE, page.getPath(), Arrays.toString(tags));
            return null;
        }
    }
}
