package com.example.NextPlan.Crawl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlScheduler {

    private final CrawlService crawlService;

    @Value("${app.crawl.schedule-enabled:false}")
    private boolean scheduleEnabled;

    @Scheduled(cron = "0 0 13 * * MON", zone = "Asia/Seoul")
    public void crawlWeekly() {
        log.info("Weekly crawl scheduler triggered. scheduleEnabled={}", scheduleEnabled);

        if (!scheduleEnabled) {
            log.info("Weekly crawl skipped because schedule is disabled.");
            return;
        }

        CrawlService.CrawlResult result = crawlService.crawlAndSave();
        log.info(
                "Weekly crawl scheduler finished. status={}, count={}, error={}",
                result.status(),
                result.count(),
                result.error()
        );
    }
}
