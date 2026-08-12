package com.marketnexus.advisorbatch.content;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ArticleContentFetcher {

    private static final Logger log = LoggerFactory.getLogger(ArticleContentFetcher.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public String fetch(String url, String fallback) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(15_000)
                    .get()
                    .body()
                    .text();
        } catch (Exception e) {
            log.warn("url={} event=content_fetch_failed error={} — falling back to RSS description", url, e.getMessage());
            return fallback;
        }
    }
}
