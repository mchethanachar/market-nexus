package com.marketnexus.newsstream.service.impl;

import com.marketnexus.newsstream.model.RawNews;
import com.marketnexus.newsstream.repository.NewsArticleRepository;
import com.marketnexus.newsstream.service.DeduplicationService;
import com.marketnexus.newsstream.util.HashUtil;
import org.springframework.stereotype.Service;

@Service
public class DeduplicationServiceImpl implements DeduplicationService {

    private final NewsArticleRepository repository;

    public DeduplicationServiceImpl(NewsArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isDuplicate(RawNews rawNews) {
        return repository.existsByUrlHash(computeHash(rawNews));
    }

    @Override
    public String computeHash(RawNews rawNews) {
        return HashUtil.sha256(rawNews.title(), rawNews.link());
    }
}
