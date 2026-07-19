package com.marketnexus.newsstream.repository;

import com.marketnexus.newsstream.model.NewsArticle;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsByUrlHash(String urlHash);

    List<NewsArticle> findByIsProcessedFalse();

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO news_articles
                (title, content, summary, url, url_hash, source_name, source_type,
                 published_at, ingested_at, is_processed)
            VALUES
                (:title, :content, :summary, :url, :urlHash, :sourceName, :sourceType,
                 :publishedAt, :ingestedAt, false)
            ON CONFLICT (url_hash) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("title")       String title,
            @Param("content")     String content,
            @Param("summary")     String summary,
            @Param("url")         String url,
            @Param("urlHash")     String urlHash,
            @Param("sourceName")  String sourceName,
            @Param("sourceType")  String sourceType,
            @Param("publishedAt") Instant publishedAt,
            @Param("ingestedAt")  Instant ingestedAt
    );

    @Modifying
    @Query("UPDATE NewsArticle a SET a.isProcessed = true WHERE a.id IN :ids")
    int markProcessedByIds(@Param("ids") List<Long> ids);
}
