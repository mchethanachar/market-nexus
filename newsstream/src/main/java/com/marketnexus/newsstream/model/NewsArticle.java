package com.marketnexus.newsstream.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "news_articles", indexes = {
        @Index(name = "idx_news_articles_url_hash", columnList = "url_hash", unique = true),
        @Index(name = "idx_news_articles_is_processed", columnList = "is_processed"),
        @Index(name = "idx_news_articles_published_at", columnList = "published_at")
})
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private String url;

    @Column(name = "url_hash", nullable = false, unique = true, length = 64)
    private String urlHash;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "is_processed", nullable = false)
    private boolean isProcessed = false;

    protected NewsArticle() {}

    public NewsArticle(String title, String content, String summary, String url,
                       String urlHash, String sourceName, String sourceType,
                       Instant publishedAt, Instant ingestedAt) {
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.url = url;
        this.urlHash = urlHash;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.publishedAt = publishedAt;
        this.ingestedAt = ingestedAt;
        this.isProcessed = false;
    }

    public static NewsArticle from(RawNews raw, String urlHash) {
        return new NewsArticle(
                raw.title(),
                null,
                raw.description(),
                raw.link(),
                urlHash,
                raw.sourceName(),
                "RSS",
                raw.publishedAt(),
                Instant.now()
        );
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSummary() { return summary; }
    public String getUrl() { return url; }
    public String getUrlHash() { return urlHash; }
    public String getSourceName() { return sourceName; }
    public String getSourceType() { return sourceType; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getIngestedAt() { return ingestedAt; }
    public boolean isProcessed() { return isProcessed; }
    public void markProcessed() { this.isProcessed = true; }
}
