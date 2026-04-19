package com.fa.core.models.impl;

import com.adobe.cq.wcm.core.components.commons.link.LinkManager;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.fa.core.models.ArticleBreadcrumb;
import com.fa.core.models.dto.BreadcrumbItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Article breadcrumb Impl
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = ArticleBreadcrumb.class, resourceType = ArticleBreadcrumbImpl.RESOURCE_TYPE, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ArticleBreadcrumbImpl implements ArticleBreadcrumb {

    public static final String RESOURCE_TYPE = "newspaper/components/structure/articlebreadcrumb";
    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private Resource resource;

    @Self
    private LinkManager linkManager;

    @Self
    @Via(type = ResourceSuperType.class)
    private Breadcrumb delegate;


    @ValueMapValue
    private String articleCategory;

    @ValueMapValue
    @Default(booleanValues = true)
    private boolean hideCurrent;

    private List<NavigationItem> items;

    @PostConstruct
    protected void init() {
        items = new ArrayList<>();
        if (currentPage == null) return;

        Page languageRoot = findLanguageRoot(currentPage);
        if (languageRoot == null) {
            return;
        }

        String languageRootPath = languageRoot.getPath();

        items.add(new BreadcrumbItem(languageRoot, false, linkManager, true));


        buildCategoryHierarchy(languageRootPath);


        if (delegate != null) {
            for (NavigationItem item : delegate.getItems()) {
                String path = item.getPath();
                if (path == null) continue;

                if (!path.startsWith(languageRootPath + "/") && !path.equals(languageRootPath)) continue;

                if (path.contains("/articles")) continue;

                if (path.matches(".*/\\d{4}(/\\d{2}){0,2}(/.*)?$")) continue;

                boolean duplicate = items.stream().anyMatch(i -> StringUtils.equals(i.getPath(), path));
                if (!duplicate) items.add(item);
            }
        }
        if (!hideCurrent) {
            items.add(new BreadcrumbItem(currentPage, true, linkManager, false));
        }

    }


    private Page findLanguageRoot(Page page) {
        Page cursor = page;
        while (cursor != null) {
            Resource content = cursor.getContentResource();
            if (content != null && StringUtils.isNotBlank(content.getValueMap().get("cq:language", String.class))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private void buildCategoryHierarchy(String languageRootPath) {
        if (StringUtils.isBlank(articleCategory)) return;

        ResourceResolver resolver = resource.getResourceResolver();
        Resource categoryResource = resolver.getResource(articleCategory);
        if (categoryResource == null) {
            return;
        }

        Page categoryPage = categoryResource.adaptTo(Page.class);
        if (categoryPage == null) return;

        boolean showHidden = resource.getValueMap().get("showHidden", false);

        List<Page> hierarchy = new ArrayList<>();
        Page cursor = categoryPage;
        while (cursor != null) {
            String cursorPath = cursor.getPath();

            if (StringUtils.equals(cursorPath, languageRootPath)) break;

            if (!cursorPath.startsWith(languageRootPath + "/")) break;

            hierarchy.add(0, cursor);
            cursor = cursor.getParent();
        }

        for (Page page : hierarchy) {
            if (StringUtils.equals(page.getPath(), currentPage.getPath())) continue;
            if (!showHidden && page.isHideInNav()) continue;

            items.add(new BreadcrumbItem(page, false, linkManager, false));
        }
    }

    @Override
    public Collection<NavigationItem> getItems() {
        return items != null ? items : Collections.emptyList();
    }


}