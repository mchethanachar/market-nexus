package com.marketnexus.newsstream.source;

import com.marketnexus.newsstream.model.RawNews;

import java.util.List;

public interface NewsSource {
    List<RawNews> fetch();
    String getName();
}
