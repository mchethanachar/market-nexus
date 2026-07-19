package com.marketnexus.newsstream.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "newsstream.feeds")
public record NewsStreamProperties(
        long pollIntervalMs,
        List<FeedSource> sources
) {
    public record FeedSource(String name, String url, boolean enabled) {}
}
