package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Model(
        adaptables = SlingHttpServletRequest.class,
        resourceType = ArticleModel.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ArticleModel {

    public static final String RESOURCE_TYPE = "newspaper/components/content/article";

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private Resource resource;

    @ValueMapValue
    private String headline;

    @ValueMapValue
    private String summary;

    @ValueMapValue
    private String byline;

    @ValueMapValue
    private String date; // author can set; ISO string recommended

    @ValueMapValue
    private String image;

    @ValueMapValue
    private String imageAlt;

    private String resolvedHeadline;
    private String resolvedSummary;
    private String resolvedByline;
    private Instant resolvedDate;
    private String resolvedImage;
    private String resolvedImageAlt;
    private List<String> tags;

    @PostConstruct
    protected void init() {
        resolvedHeadline = StringUtils.firstNonBlank(
                headline,
                currentPage != null ? currentPage.getPageTitle() : null,
                currentPage != null ? currentPage.getTitle() : null,
                currentPage != null ? currentPage.getName() : null
        );

        resolvedSummary = StringUtils.firstNonBlank(
                summary,
                currentPage != null ? currentPage.getDescription() : null
        );

        resolvedByline = StringUtils.defaultIfBlank(byline, "");

        resolvedDate = parseDate(date, currentPage);

        resolvedImage = StringUtils.defaultIfBlank(image, "");
        resolvedImageAlt = StringUtils.defaultIfBlank(imageAlt, resolvedHeadline);

        tags = readTags(currentPage);
    }

    public String getId() {
        return "cmp-article-" + Math.abs((resource != null ? resource.getPath() : "").hashCode());
    }

    public String getHeadline() {
        return resolvedHeadline;
    }

    public String getSummary() {
        return resolvedSummary;
    }

    public String getByline() {
        return resolvedByline;
    }

    public Instant getDate() {
        return resolvedDate;
    }

    public String getImage() {
        return resolvedImage;
    }

    public String getImageAlt() {
        return resolvedImageAlt;
    }

    public List<String> getTags() {
        return tags;
    }

    private static Instant parseDate(String configured, Page page) {
        if (StringUtils.isNotBlank(configured)) {
            try {
                return Instant.parse(configured);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }

        if (page == null) {
            return null;
        }

        // Common props for article-like pages
        ValueMap vm = page.getProperties();
        Object published = vm.get("cq:lastModified", Object.class);
        if (published instanceof java.util.Calendar) {
            return ((java.util.Calendar) published).toInstant();
        }
        return null;
    }

    private static List<String> readTags(Page page) {
        if (page == null) {
            return Collections.emptyList();
        }
        Resource content = page.getContentResource();
        if (content == null) {
            return Collections.emptyList();
        }
        String[] cqTags = content.getValueMap().get("cq:tags", String[].class);
        if (cqTags == null || cqTags.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(cqTags);
    }
}

