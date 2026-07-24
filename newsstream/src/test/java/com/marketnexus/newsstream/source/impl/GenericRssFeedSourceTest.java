package com.marketnexus.newsstream.source.impl;

import com.marketnexus.newsstream.model.RawNews;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GenericRssFeedSourceTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private String feedUrl() {
        return "http://localhost:" + port + "/rss";
    }

    private void serveRssBody(String body, int status) {
        server.createContext("/rss", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (var os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.getResponseBody().close();
            }
        });
        server.start();
    }

    @Test
    void fetch_parsesWellFormedFeedWithMultipleItems() {
        serveRssBody("""
                <?xml version="1.0"?>
                <rss version="2.0">
                  <channel>
                    <title>Test Feed</title>
                    <item>
                      <title>Article One</title>
                      <link>http://example.com/one</link>
                      <description>First description</description>
                      <pubDate>Mon, 01 Jan 2024 10:00:00 GMT</pubDate>
                    </item>
                    <item>
                      <title>Article Two</title>
                      <link>http://example.com/two</link>
                      <description>Second description</description>
                      <pubDate>Tue, 02 Jan 2024 11:00:00 GMT</pubDate>
                    </item>
                  </channel>
                </rss>
                """, 200);

        List<RawNews> result = new GenericRssFeedSource("test-source", feedUrl()).fetch();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Article One");
        assertThat(result.get(0).link()).isEqualTo("http://example.com/one");
        assertThat(result.get(0).description()).isEqualTo("First description");
        assertThat(result.get(0).sourceName()).isEqualTo("test-source");
        assertThat(result.get(0).publishedAt()).isNotNull();
        assertThat(result.get(1).title()).isEqualTo("Article Two");
    }

    @Test
    void fetch_sanitizesDuplicateDoctypeAndBareAmpersand() {
        serveRssBody("""
                <?xml version="1.0"?>
                <!DOCTYPE rss>
                <!DOCTYPE rss>
                <rss version="2.0">
                  <channel>
                    <title>Test Feed</title>
                    <item>
                      <title>Fish & Chips</title>
                      <link>http://example.com/fish</link>
                      <description>Tasty & fresh</description>
                      <pubDate>Mon, 01 Jan 2024 10:00:00 GMT</pubDate>
                    </item>
                  </channel>
                </rss>
                """, 200);

        List<RawNews> result = new GenericRssFeedSource("test-source", feedUrl()).fetch();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Fish & Chips");
        assertThat(result.get(0).description()).isEqualTo("Tasty & fresh");
    }

    @Test
    void fetch_returnsEmptyListWhenServerReturnsError() {
        serveRssBody("", 500);

        List<RawNews> result = new GenericRssFeedSource("test-source", feedUrl()).fetch();

        assertThat(result).isEmpty();
    }

    @Test
    void fetch_sendsSpoofedBrowserUserAgent() {
        AtomicReference<String> capturedUserAgent = new AtomicReference<>();
        String rss = """
                <?xml version="1.0"?>
                <rss version="2.0">
                  <channel>
                    <title>Test Feed</title>
                  </channel>
                </rss>
                """;
        server.createContext("/rss", exchange -> {
            capturedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            byte[] bytes = rss.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        new GenericRssFeedSource("test-source", feedUrl()).fetch();

        assertThat(capturedUserAgent.get()).contains("Mozilla/5.0");
    }

    @Test
    void fetch_entryWithoutPublishedDateFallsBackToNow() {
        serveRssBody("""
                <?xml version="1.0"?>
                <rss version="2.0">
                  <channel>
                    <title>Test Feed</title>
                    <item>
                      <title>No Date Article</title>
                      <link>http://example.com/nodate</link>
                      <description>No date here</description>
                    </item>
                  </channel>
                </rss>
                """, 200);

        Instant before = Instant.now();
        List<RawNews> result = new GenericRssFeedSource("test-source", feedUrl()).fetch();
        Instant after = Instant.now();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).publishedAt()).isBetween(before, after);
    }

    @Test
    void fetch_entryWithoutDescriptionIsNull() {
        serveRssBody("""
                <?xml version="1.0"?>
                <rss version="2.0">
                  <channel>
                    <title>Test Feed</title>
                    <item>
                      <title>No Description Article</title>
                      <link>http://example.com/nodesc</link>
                      <pubDate>Mon, 01 Jan 2024 10:00:00 GMT</pubDate>
                    </item>
                  </channel>
                </rss>
                """, 200);

        List<RawNews> result = new GenericRssFeedSource("test-source", feedUrl()).fetch();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isNull();
    }
}
