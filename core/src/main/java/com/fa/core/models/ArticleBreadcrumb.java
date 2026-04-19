package com.fa.core.models;

import com.adobe.cq.wcm.core.components.models.NavigationItem;

import java.util.Collection;
import java.util.List;

/**
 * Article Breadcrumb Interface
 */
public interface ArticleBreadcrumb {
    Collection<NavigationItem> getItems();
}
