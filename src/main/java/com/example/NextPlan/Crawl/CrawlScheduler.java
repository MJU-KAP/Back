package com.example.NextPlan.Crawl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrawlScheduler {

    private final CrawlService crawlService;

    @Value("${app.crawl.schedule-enabled:false}")
    private boolean scheduleEnabled;

    @Scheduled(cron = "0 0 3 * * MON", zone = "Asia/Seoul")
    public void crawlWeekly() {
        if (!scheduleEnabled) {
            return;
        }

        crawlService.crawlAndSave();
    }
}
