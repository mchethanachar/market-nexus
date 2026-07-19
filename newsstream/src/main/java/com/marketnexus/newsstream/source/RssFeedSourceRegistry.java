package com.marketnexus.newsstream.source;

import com.marketnexus.newsstream.config.NewsStreamProperties;
import com.marketnexus.newsstream.source.impl.GenericRssFeedSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RssFeedSourceRegistry {

    private final List<NewsSource> sources;

    public RssFeedSourceRegistry(NewsStreamProperties properties) {
        this.sources = properties.sources().stream()
                .filter(NewsStreamProperties.FeedSource::enabled)
                .<NewsSource>map(s -> new GenericRssFeedSource(s.name(), s.url()))
                .toList();
    }

    public List<NewsSource> getSources() {
        return sources;
    }
}
