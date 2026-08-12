package com.marketnexus.advisorbatch.service;

import com.marketnexus.advisorbatch.ai.InvestmentAdvisor;
import com.marketnexus.advisorbatch.content.ArticleContentFetcher;
import com.marketnexus.advisorbatch.dedup.SeenArticles;
import com.marketnexus.advisorbatch.model.Article;
import com.marketnexus.advisorbatch.model.RawArticle;
import com.marketnexus.advisorbatch.source.RssReader;
import com.marketnexus.advisorbatch.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightService.class);
    private static final int ARTICLES_PER_SOURCE = 5;

    private record Source(String name, String url) {}

    private static final List<Source> SOURCES = List.of(
            new Source("livemint", "https://www.livemint.com/rss/markets"),
            new Source("hindu-business-line", "https://www.thehindubusinessline.com/markets/?service=rss"),
            new Source("bloomberg", "https://www.bloomberg.com/feeds/markets/news.rss"),
            new Source("financial-times", "https://www.ft.com/markets?format=rss"),
            new Source("cnbc", "https://www.cnbc.com/id/10000664/device/rss/rss.html"),
            new Source("marketwatch", "https://feeds.content.dowjones.io/public/rss/mw_topstories"),
            new Source("economist", "https://www.economist.com/finance-and-economics/rss.xml"),
            new Source("seeking-alpha", "https://seekingalpha.com/feed.xml"),
            new Source("oilprice", "https://oilprice.com/rss/main")
    );

    private final RssReader rssReader;
    private final ArticleContentFetcher contentFetcher;
    private final SeenArticles seenArticles;
    private final InvestmentAdvisor investmentAdvisor;

    public InsightService(RssReader rssReader,
                           ArticleContentFetcher contentFetcher,
                           SeenArticles seenArticles,
                           InvestmentAdvisor investmentAdvisor) {
        this.rssReader = rssReader;
        this.contentFetcher = contentFetcher;
        this.seenArticles = seenArticles;
        this.investmentAdvisor = investmentAdvisor;
    }

    public void runCycle() {
        List<Article> newThisCycle = new ArrayList<>();

        for (Source source : SOURCES) {
            List<RawArticle> fetched = rssReader.fetch(source.name(), source.url(), ARTICLES_PER_SOURCE);
            int newCount = 0, duplicateCount = 0;

            for (RawArticle raw : fetched) {
                String id = HashUtil.sha256(raw.title(), raw.link());
                if (!seenArticles.isNew(id)) {
                    duplicateCount++;
                    continue;
                }
                String content = contentFetcher.fetch(raw.link(), raw.description());
                newThisCycle.add(Article.from(raw, id, content));
                newCount++;
            }

            log.info("source={} fetched={} new={} duplicateSkipped={}",
                    source.name(), fetched.size(), newCount, duplicateCount);
        }

        if (newThisCycle.isEmpty()) {
            log.info("No new articles this cycle — skipping AI call");
            return;
        }

        log.info("Sending {} new articles to Claude for synthesis", newThisCycle.size());
        String insight = investmentAdvisor.synthesize(newThisCycle);
        log.info("Investing insight for this cycle:\n{}", insight);
    }
}
