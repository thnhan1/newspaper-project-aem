package com.fa.core.models;

import java.util.List;
import com.fa.core.models.dto.TagItem;

/**
 * Interface for Tags List component
 */
public interface TagsListModel {
    /**
     * Gets the list of tags applied to the current page.
     *
     * @return List of Tags
     */
    List<TagItem> getTags();
    
    /**
     * Checks if the list has any tags.
     * 
     * @return true if tags are present
     */
    boolean isEmpty();
}
