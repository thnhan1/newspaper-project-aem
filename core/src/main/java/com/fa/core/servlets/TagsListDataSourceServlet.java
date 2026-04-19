package com.fa.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.tagging.Tag;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component(
    service = Servlet.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Tags List Data Source Servlet",
        "sling.servlet.resourceTypes=" + TagsListDataSourceServlet.RESOURCE_TYPE,
        "sling.servlet.methods=" + HttpConstants.METHOD_GET
    }
)
public class TagsListDataSourceServlet extends SlingSafeMethodsServlet {

    public static final String RESOURCE_TYPE = "newspaper/components/content/tagslist/datasource/pagetags";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        ResourceResolver resolver = request.getResourceResolver();
        
        String contentPath = (String) request.getAttribute("granite.ui.form.contentpath");
        if (contentPath == null) {
            contentPath = request.getRequestPathInfo().getSuffix();
        }

        List<Resource> fakeResourceList = new ArrayList<>();

        if (contentPath != null) {
            Resource componentResource = resolver.getResource(contentPath);
            if (componentResource != null) {
                PageManager pageManager = resolver.adaptTo(PageManager.class);
                if (pageManager != null) {
                    Page page = pageManager.getContainingPage(componentResource);
                    if (page != null) {
                        java.util.Locale locale = page.getLanguage(false);
                        Tag[] tags = page.getTags();
                        if (tags != null) {
                            for (Tag tag : tags) {
                                String title = tag.getTitle(locale);
                                if (title == null) {
                                    title = tag.getTitle();
                                }
                                ValueMap vm = new ValueMapDecorator(new HashMap<>());
                                vm.put("value", tag.getTagID());
                                vm.put("text", title + " (" + tag.getName() + ")");
                                fakeResourceList.add(new ValueMapResource(resolver, new ResourceMetadata(), "nt:unstructured", vm));
                            }
                        }
                    }
                }
            }
        }

        DataSource ds = new SimpleDataSource(fakeResourceList.iterator());
        request.setAttribute(DataSource.class.getName(), ds);
    }
}
