package com.fa.core.models.impl;

import com.day.cq.tagging.Tag;
import com.day.cq.wcm.api.Page;
import com.fa.core.models.TagsListModel;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import com.fa.core.models.dto.TagItem;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of the Tags List component
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = TagsListModel.class,
    resourceType = TagsListModelImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class TagsListModelImpl implements TagsListModel {

    public static final String RESOURCE_TYPE = "newspaper/components/content/tagslist";

    @ScriptVariable
    private Page currentPage;

    @ValueMapValue
    private String[] excludedTags;

    private List<TagItem> tags;

    @PostConstruct
    protected void init() {
        tags = new ArrayList<>();
        if (currentPage != null) {
            Locale pageLocale = currentPage.getLanguage(false);
            Tag[] pageTags = currentPage.getTags();
            if (pageTags != null && pageTags.length > 0) {
                List<Tag> filteredTags = new ArrayList<>();
                if (excludedTags != null && excludedTags.length > 0) {
                    List<String> excludedList = Arrays.asList(excludedTags);
                    Arrays.stream(pageTags)
                          .filter(t -> !excludedList.contains(t.getTagID()))
                          .forEach(filteredTags::add);
                } else {
                    filteredTags.addAll(Arrays.asList(pageTags));
                }
                
                filteredTags.forEach(t -> tags.add(mapToDto(t, pageLocale)));
            }
        }
    }

    private TagItem mapToDto(Tag tag, Locale locale) {
        String title = tag.getTitle(locale);
        if (title == null) {
            title = tag.getTitle();
        }
        return new TagItem(tag.getTagID(), title);
    }

    @Override
    public List<TagItem> getTags() {
        return List.copyOf(tags); // Java 11 immutable list
    }

    @Override
    public boolean isEmpty() {
        return tags == null || tags.isEmpty();
    }
}
