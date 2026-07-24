package com.marketnexus.newsstream.source;

import com.marketnexus.newsstream.config.NewsStreamProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RssFeedSourceRegistryTest {

    @Test
    void getSources_includesOnlyEnabledFeeds() {
        NewsStreamProperties properties = new NewsStreamProperties(300_000, List.of(
                new NewsStreamProperties.FeedSource("enabled-one", "http://example.com/one", true),
                new NewsStreamProperties.FeedSource("disabled-one", "http://example.com/disabled", false),
                new NewsStreamProperties.FeedSource("enabled-two", "http://example.com/two", true)
        ));

        RssFeedSourceRegistry registry = new RssFeedSourceRegistry(properties);

        assertThat(registry.getSources())
                .hasSize(2)
                .extracting(NewsSource::getName)
                .containsExactly("enabled-one", "enabled-two");
    }

    @Test
    void getSources_returnsEmptyListWhenNoFeedsConfigured() {
        NewsStreamProperties properties = new NewsStreamProperties(300_000, List.of());

        RssFeedSourceRegistry registry = new RssFeedSourceRegistry(properties);

        assertThat(registry.getSources()).isEmpty();
    }

    @Test
    void getSources_returnsEmptyListWhenAllFeedsDisabled() {
        NewsStreamProperties properties = new NewsStreamProperties(300_000, List.of(
                new NewsStreamProperties.FeedSource("disabled-one", "http://example.com/one", false)
        ));

        RssFeedSourceRegistry registry = new RssFeedSourceRegistry(properties);

        assertThat(registry.getSources()).isEmpty();
    }
}
