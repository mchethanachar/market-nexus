package com.marketnexus.newsstream.model;

import java.time.Instant;

public record RawNews(
        String title,
        String link,
        String description,
        Instant publishedAt,
        String sourceName
) {}
