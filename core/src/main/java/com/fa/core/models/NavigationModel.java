package com.fa.core.models;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Navigation;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
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
import java.util.Iterator;
import java.util.List;

/**
 * Custom Navigation Model extending AEM Core Components Navigation
 * Provides multi-level navigation with MSM support
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = Navigation.class,
        resourceType = NavigationModel.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class NavigationModel implements Navigation {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationModel.class);

    protected static final String RESOURCE_TYPE = "newspaper/components/structure/navigation";

    @Self
    @Via(type = ResourceSuperType.class)
    private Navigation delegate;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private Resource resource;

    private List<NavigationItem> items;
    private String accessibilityLabel;

    @PostConstruct
    protected void init() {
        LOG.debug("Initializing NavigationModel for page: {}", 
                  currentPage != null ? currentPage.getPath() : "null");

        if (delegate != null) {
            // Get items from Core Component and enhance with children
            Collection<NavigationItem> delegateItems = delegate.getItems();
            if (delegateItems != null && !delegateItems.isEmpty()) {
                items = new ArrayList<>();
                for (NavigationItem item : delegateItems) {
                    items.add(new EnhancedNavigationItem(item, currentPage));
                }
                LOG.debug("Enhanced {} navigation items with children", items.size());
            }
        }

        // Set default accessibility label
        accessibilityLabel = "Main Navigation";
    }

    @Override
    public List<NavigationItem> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    @Override
    public String getAccessibilityLabel() {
        return delegate != null ? delegate.getAccessibilityLabel() : accessibilityLabel;
    }

    @Override
    public String getId() {
        return delegate != null ? delegate.getId() : 
               "cmp-navigation-" + Math.abs(resource.getPath().hashCode());
    }

    /**
     * Enhanced NavigationItem that wraps Core Component item and adds children support
     */
    private static class EnhancedNavigationItem implements NavigationItem {

        private final NavigationItem delegate;
        private List<NavigationItem> children;

        EnhancedNavigationItem(NavigationItem delegate, Page currentPage) {
            this.delegate = delegate;
            this.children = null; // Lazy load
        }

        @Override
        public Page getPage() {
            Link<Page> link = getLink();
            return link != null ? link.getReference() : null;
        }

        @Override
        public boolean isActive() {
            return delegate.isActive();
        }

        @Override
        public boolean isCurrent() {
            return delegate.isCurrent();
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

        /**
         * Load children pages respecting hideInNav
         */
        private List<NavigationItem> loadChildren() {
            Link<Page> link = getLink();
            Page page = link != null ? link.getReference() : null;
            
            if (page == null) {
                return Collections.emptyList();
            }

            List<NavigationItem> childList = new ArrayList<>();
            PageFilter pageFilter = new PageFilter(false, true);

            Iterator<Page> children = page.listChildren(pageFilter);
            while (children.hasNext()) {
                Page child = children.next();
                if (!child.isHideInNav()) {
                    childList.add(new SimpleNavigationItem(child));
                }
            }

            LOG.debug("Loaded {} children for page: {}", childList.size(), page.getPath());
            return childList;
        }
    }

    /**
     * Simple NavigationItem for child pages
     */
    private static class SimpleNavigationItem implements NavigationItem {

        private final Page page;

        SimpleNavigationItem(Page page) {
            this.page = page;
        }

        @Override
        public Page getPage() {
            return page;
        }

        @Override
        public boolean isActive() {
            return false; // Simplified for Level 2
        }

        @Override
        public boolean isCurrent() {
            return false; // Simplified for Level 2
        }

        @Override
        public com.adobe.cq.wcm.core.components.commons.link.Link<Page> getLink() {
            return null; // Simple implementation, HTL will use getURL()
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
            if (StringUtils.isNotBlank(title)) {
                return title;
            }
            return page.getName();
        }

        @Override
        public int getLevel() {
            return page.getDepth();
        }

        @Override
        public List<NavigationItem> getChildren() {
            return Collections.emptyList(); // Level 2 only for now
        }

        @Override
        public Resource getTeaserResource() {
            return null;
        }
    }
}
