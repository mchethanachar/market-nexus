package com.marketnexus.newsstream.service.impl;

import com.marketnexus.newsstream.model.NewsArticle;
import com.marketnexus.newsstream.model.RawNews;
import com.marketnexus.newsstream.repository.NewsArticleRepository;
import com.marketnexus.newsstream.service.DeduplicationService;
import com.marketnexus.newsstream.service.IngestionService;
import com.marketnexus.newsstream.source.NewsSource;
import com.marketnexus.newsstream.source.RssFeedSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestionServiceImpl implements IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionServiceImpl.class);

    private final RssFeedSourceRegistry registry;
    private final DeduplicationService deduplicationService;
    private final NewsArticleRepository repository;

    public IngestionServiceImpl(RssFeedSourceRegistry registry,
                                DeduplicationService deduplicationService,
                                NewsArticleRepository repository) {
        this.registry = registry;
        this.deduplicationService = deduplicationService;
        this.repository = repository;
    }

    @Override
    public void ingestAll() {
        List<NewsSource> sources = registry.getSources();
        int totalFetched = 0, totalSaved = 0, totalSkipped = 0;

        for (NewsSource source : sources) {
            IngestionResult result = ingestSource(source);
            totalFetched += result.fetched();
            totalSaved   += result.saved();
            totalSkipped += result.skipped();
        }

        log.info("Ingestion cycle complete | sources={} fetched={} saved={} skipped={}",
                sources.size(), totalFetched, totalSaved, totalSkipped);
    }

    private IngestionResult ingestSource(NewsSource source) {
        List<RawNews> articles;
        try {
            articles = source.fetch();
        } catch (Exception e) {
            log.error("source={} event=fetch_failed error={}", source.getName(), e.getMessage(), e);
            return new IngestionResult(0, 0, 0);
        }

        int saved = 0, skipped = 0;

        for (RawNews raw : articles) {
            if (deduplicationService.isDuplicate(raw)) {
                skipped++;
                continue;
            }
            persist(raw);
            saved++;
        }

        log.info("source={} fetched={} saved={} skipped={}",
                source.getName(), articles.size(), saved, skipped);

        return new IngestionResult(articles.size(), saved, skipped);
    }

    private void persist(RawNews raw) {
        String hash = deduplicationService.computeHash(raw);
        NewsArticle article = NewsArticle.from(raw, hash);
        repository.insertIfAbsent(
                article.getTitle(), article.getContent(), article.getSummary(),
                article.getUrl(), article.getUrlHash(), article.getSourceName(),
                article.getSourceType(), article.getPublishedAt(), article.getIngestedAt()
        );
    }

    private record IngestionResult(int fetched, int saved, int skipped) {}
}
