package com.fa.core.models.dto;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.commons.link.LinkManager;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

/**
 * Breadcrumb Item
 */
public class BreadcrumbItem implements NavigationItem {

    private final Page page;
    private final boolean active;
    private final Link<Page> link;
    private final boolean isRoot;

    public BreadcrumbItem(Page page, boolean active, LinkManager linkManager, boolean isRoot) {
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

    @Override
    public String getTitle() {
        return StringUtils.firstNonBlank(page.getNavigationTitle(), page.getPageTitle(), page.getTitle(), page.getName());
    }
}