package com.fa.core.services;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

public interface ArticleQueryService {

    enum QueryMode {
        TAGS,
        FOLDER
    }

    final class QueryParams {
        private final QueryMode mode;
        private final String rootPath;
        private final List<String> categoryTags;
        private final int limit;
        private final boolean sortAscending;
        private final String featuredArticlePath;
        private final boolean excludeFeaturedFromList;

        public QueryParams(QueryMode mode,
                           String rootPath,
                           List<String> categoryTags,
                           int limit,
                           boolean sortAscending,
                           String featuredArticlePath,
                           boolean excludeFeaturedFromList) {
            this.mode = mode;
            this.rootPath = rootPath;
            this.categoryTags = categoryTags;
            this.limit = limit;
            this.sortAscending = sortAscending;
            this.featuredArticlePath = featuredArticlePath;
            this.excludeFeaturedFromList = excludeFeaturedFromList;
        }

        public QueryMode getMode() {
            return mode;
        }

        public String getRootPath() {
            return rootPath;
        }

        public List<String> getCategoryTags() {
            return categoryTags;
        }

        public int getLimit() {
            return limit;
        }

        public boolean isSortAscending() {
            return sortAscending;
        }

        public String getFeaturedArticlePath() {
            return featuredArticlePath;
        }

        public boolean isExcludeFeaturedFromList() {
            return excludeFeaturedFromList;
        }
    }

    List<ArticleSummary> getArticles(ResourceResolver resourceResolver, QueryParams params);
}

