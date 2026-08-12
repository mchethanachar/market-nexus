package com.marketnexus.advisorbatch.scheduler;

import com.marketnexus.advisorbatch.service.InsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InsightScheduler {

    private static final Logger log = LoggerFactory.getLogger(InsightScheduler.class);

    private final InsightService insightService;

    public InsightScheduler(InsightService insightService) {
        this.insightService = insightService;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void poll() {
        log.info("Insight cycle started");
        try {
            insightService.runCycle();
        } catch (Exception e) {
            log.error("Insight cycle failed: {}", e.getMessage(), e);
        }
        log.info("Insight cycle completed");
    }
}
