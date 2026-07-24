package com.marketnexus.newsstream.service.impl;

import com.marketnexus.newsstream.model.RawNews;
import com.marketnexus.newsstream.repository.NewsArticleRepository;
import com.marketnexus.newsstream.util.HashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceImplTest {

    @Mock
    private NewsArticleRepository repository;

    private DeduplicationServiceImpl deduplicationService;

    private static final RawNews RAW_NEWS = new RawNews(
            "Sensex hits new high", "http://example.com/article", "description",
            Instant.now(), "test-source");

    @Test
    void isDuplicate_returnsTrueWhenHashAlreadyExists() {
        deduplicationService = new DeduplicationServiceImpl(repository);
        when(repository.existsByUrlHash(HashUtil.sha256(RAW_NEWS.title(), RAW_NEWS.link()))).thenReturn(true);

        assertThat(deduplicationService.isDuplicate(RAW_NEWS)).isTrue();
    }

    @Test
    void isDuplicate_returnsFalseWhenHashIsNew() {
        deduplicationService = new DeduplicationServiceImpl(repository);
        when(repository.existsByUrlHash(HashUtil.sha256(RAW_NEWS.title(), RAW_NEWS.link()))).thenReturn(false);

        assertThat(deduplicationService.isDuplicate(RAW_NEWS)).isFalse();
    }

    @Test
    void isDuplicate_checksRepositoryUsingTitleAndLinkHash() {
        deduplicationService = new DeduplicationServiceImpl(repository);
        when(repository.existsByUrlHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        deduplicationService.isDuplicate(RAW_NEWS);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).existsByUrlHash(hashCaptor.capture());
        assertThat(hashCaptor.getValue()).isEqualTo(HashUtil.sha256(RAW_NEWS.title(), RAW_NEWS.link()));
    }

    @Test
    void computeHash_matchesHashUtilContract() {
        deduplicationService = new DeduplicationServiceImpl(repository);

        assertThat(deduplicationService.computeHash(RAW_NEWS))
                .isEqualTo(HashUtil.sha256(RAW_NEWS.title(), RAW_NEWS.link()));
    }
}
