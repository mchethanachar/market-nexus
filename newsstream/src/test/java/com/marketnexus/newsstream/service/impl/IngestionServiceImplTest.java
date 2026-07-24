package com.marketnexus.newsstream.service.impl;

import com.marketnexus.newsstream.model.RawNews;
import com.marketnexus.newsstream.repository.NewsArticleRepository;
import com.marketnexus.newsstream.service.DeduplicationService;
import com.marketnexus.newsstream.source.NewsSource;
import com.marketnexus.newsstream.source.RssFeedSourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {

    @Mock
    private RssFeedSourceRegistry registry;
    @Mock
    private DeduplicationService deduplicationService;
    @Mock
    private NewsArticleRepository repository;

    private IngestionServiceImpl ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new IngestionServiceImpl(registry, deduplicationService, repository);
    }

    private static RawNews rawNews(String title, String link) {
        return new RawNews(title, link, "description for " + title, Instant.now(), "test-source");
    }

    @Test
    void ingestAll_withNoSources_completesWithoutTouchingRepository() {
        when(registry.getSources()).thenReturn(List.of());

        ingestionService.ingestAll();

        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void ingestAll_savesOnlyNonDuplicateArticlesWithCorrectMapping() {
        RawNews duplicate = rawNews("Duplicate Article", "http://example.com/dup");
        RawNews fresh = rawNews("Fresh Article", "http://example.com/fresh");

        NewsSource source = mock(NewsSource.class);
        when(source.getName()).thenReturn("test-source");
        when(source.fetch()).thenReturn(List.of(duplicate, fresh));
        when(registry.getSources()).thenReturn(List.of(source));

        when(deduplicationService.isDuplicate(duplicate)).thenReturn(true);
        when(deduplicationService.isDuplicate(fresh)).thenReturn(false);
        when(deduplicationService.computeHash(fresh)).thenReturn("fresh-hash");

        ingestionService.ingestAll();

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sourceTypeCaptor = ArgumentCaptor.forClass(String.class);

        verify(repository, times(1)).insertIfAbsent(
                titleCaptor.capture(), any(), any(), urlCaptor.capture(),
                hashCaptor.capture(), any(), sourceTypeCaptor.capture(), any(), any());

        assertThat(titleCaptor.getValue()).isEqualTo("Fresh Article");
        assertThat(urlCaptor.getValue()).isEqualTo("http://example.com/fresh");
        assertThat(hashCaptor.getValue()).isEqualTo("fresh-hash");
        assertThat(sourceTypeCaptor.getValue()).isEqualTo("RSS");
    }

    @Test
    void ingestAll_aggregatesSavedCountAcrossMultipleSources() {
        RawNews sourceOneArticle = rawNews("Source One Article", "http://example.com/one");
        RawNews sourceTwoDuplicate = rawNews("Source Two Duplicate", "http://example.com/two-dup");
        RawNews sourceTwoArticle = rawNews("Source Two Article", "http://example.com/two");

        NewsSource sourceOne = mock(NewsSource.class);
        when(sourceOne.getName()).thenReturn("source-one");
        when(sourceOne.fetch()).thenReturn(List.of(sourceOneArticle));

        NewsSource sourceTwo = mock(NewsSource.class);
        when(sourceTwo.getName()).thenReturn("source-two");
        when(sourceTwo.fetch()).thenReturn(List.of(sourceTwoDuplicate, sourceTwoArticle));

        when(registry.getSources()).thenReturn(List.of(sourceOne, sourceTwo));
        when(deduplicationService.isDuplicate(sourceOneArticle)).thenReturn(false);
        when(deduplicationService.isDuplicate(sourceTwoDuplicate)).thenReturn(true);
        when(deduplicationService.isDuplicate(sourceTwoArticle)).thenReturn(false);

        ingestionService.ingestAll();

        verify(repository, times(2)).insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
