package com.marketnexus.advisorbatch.model;

import java.time.Instant;

public record RawArticle(
        String title,
        String link,
        String description,
        Instant publishedAt,
        String sourceName
) {}
