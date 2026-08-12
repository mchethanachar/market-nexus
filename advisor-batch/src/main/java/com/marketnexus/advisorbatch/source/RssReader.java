package com.marketnexus.advisorbatch.source;

import com.marketnexus.advisorbatch.model.RawArticle;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
public class RssReader {

    private static final Logger log = LoggerFactory.getLogger(RssReader.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public List<RawArticle> fetch(String sourceName, String feedUrl, int limit) {
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
                    .limit(limit)
                    .map(entry -> toRawArticle(entry, sourceName))
                    .toList();
        } catch (Exception e) {
            log.error("source={} event=fetch_failed error={}", sourceName, e.getMessage());
            return Collections.emptyList();
        }
    }

    private RawArticle toRawArticle(SyndEntry entry, String sourceName) {
        return new RawArticle(
                entry.getTitle(),
                entry.getLink(),
                entry.getDescription() != null ? entry.getDescription().getValue() : null,
                entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant() : Instant.now(),
                sourceName
        );
    }
}
