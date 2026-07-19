package com.marketnexus.newsstream.source.impl;

import com.marketnexus.newsstream.model.RawNews;
import com.marketnexus.newsstream.source.NewsSource;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class GenericRssFeedSource implements NewsSource {

    private static final Logger log = LoggerFactory.getLogger(GenericRssFeedSource.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final String name;
    private final String feedUrl;

    public GenericRssFeedSource(String name, String feedUrl) {
        this.name = name;
        this.feedUrl = feedUrl;
    }

    @Override
    public List<RawNews> fetch() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(feedUrl).openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);

            // Some feeds emit malformed XML: duplicate DOCTYPEs or bare & characters.
            String raw = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String cleaned = raw
                    .replaceAll("<!DOCTYPE[^>]*>", "")
                    .replaceAll("&(?!(?:#[0-9]+|#x[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);)", "&amp;");

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(
                    new ByteArrayInputStream(cleaned.getBytes(StandardCharsets.UTF_8))));

            return feed.getEntries().stream()
                    .map(this::toRawNews)
                    .toList();
        } catch (Exception e) {
            log.error("source={} event=fetch_failed error={}", name, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    private RawNews toRawNews(SyndEntry entry) {
        return new RawNews(
                entry.getTitle(),
                entry.getLink(),
                entry.getDescription() != null ? entry.getDescription().getValue() : null,
                entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant() : Instant.now(),
                name
        );
    }
}
