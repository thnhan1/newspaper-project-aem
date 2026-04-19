package com.fa.core.models.dto;

/**
 * DTO for displaying localized tags
 */
public class TagItem {
    private final String tagID;
    private final String title;

    public TagItem(String tagID, String title) {
        this.tagID = tagID;
        this.title = title;
    }

    public String getTagID() {
        return tagID;
    }

    public String getTitle() {
        return title;
    }
}
