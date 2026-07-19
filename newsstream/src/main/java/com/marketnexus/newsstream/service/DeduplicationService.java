package com.marketnexus.newsstream.service;

import com.marketnexus.newsstream.model.RawNews;

public interface DeduplicationService {
    boolean isDuplicate(RawNews rawNews);
    String computeHash(RawNews rawNews);
}
