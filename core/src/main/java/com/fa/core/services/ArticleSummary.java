package com.fa.core.services;

import java.time.Instant;

public final class ArticleSummary {
    private final String title;
    private final String url;
    private final String description;
    private final Instant date;
    private final String path;
    private final String image;
    private final String imageAlt;

    public ArticleSummary(String title,
                          String url,
                          String description,
                          Instant date,
                          String path,
                          String image,
                          String imageAlt) {
        this.title = title;
        this.url = url;
        this.description = description;
        this.date = date;
        this.path = path;
        this.image = image;
        this.imageAlt = imageAlt;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public Instant getDate() {
        return date;
    }

    public String getPath() {
        return path;
    }

    public String getImage() {
        return image;
    }

    public String getImageAlt() {
        return imageAlt;
    }
}

