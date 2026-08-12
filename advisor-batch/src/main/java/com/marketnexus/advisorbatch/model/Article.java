package com.marketnexus.advisorbatch.model;

import java.time.Instant;

public record Article(
        String id,
        String title,
        String link,
        String content,
        Instant publishedAt,
        String sourceName
) {
    public static Article from(RawArticle raw, String id, String content) {
        return new Article(id, raw.title(), raw.link(), content, raw.publishedAt(), raw.sourceName());
    }
}
