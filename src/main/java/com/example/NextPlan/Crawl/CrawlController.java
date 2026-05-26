package com.example.NextPlan.Crawl;

import com.example.NextPlan.Crawl.CrawlService.CrawlResult;
import com.example.NextPlan.Crawl.CrawlService.CrawlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/crawl", "/api/boards"})
@RequiredArgsConstructor
public class CrawlController {

    private final CrawlService crawlService;

    @GetMapping
    public CrawlResponse getCrawlData(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return crawlService.getCrawlData(category, page, size, sort);
    }

    @PostMapping("/refresh")
    public CrawlResult refreshCrawlData() {
        return crawlService.crawlAndSave();
    }
}
