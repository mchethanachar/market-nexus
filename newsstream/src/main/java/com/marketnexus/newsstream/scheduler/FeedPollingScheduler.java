package com.marketnexus.newsstream.scheduler;

import com.marketnexus.newsstream.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FeedPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(FeedPollingScheduler.class);

    private final IngestionService ingestionService;

    public FeedPollingScheduler(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Scheduled(fixedDelayString = "${newsstream.feeds.poll-interval-ms:300000}")
    public void poll() {
        log.info("Feed poll cycle started");
        try {
            ingestionService.ingestAll();
        } catch (Exception e) {
            log.error("Feed poll cycle failed: {}", e.getMessage(), e);
        }
        log.info("Feed poll cycle completed");
    }
}
