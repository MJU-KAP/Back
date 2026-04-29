package com.example.NextPlan.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.beans.factory.annotation.Value;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<CrawlTarget> TARGETS = List.of(
            new CrawlTarget(
                    "activity",
                    "대외활동",
                    "https://linkareer.com/list/activity?filterBy_interestIDs=13&filterType=INTEREST&orderBy_direction=DESC&orderBy_field=CREATED_AT&page=1"
            ),
            new CrawlTarget(
                    "contest",
                    "공모전",
                    "https://linkareer.com/list/contest?filterBy_categoryIDs=35&filterBy_categoryIDs=41&filterType=CATEGORY&orderBy_direction=DESC&orderBy_field=CREATED_AT&page=1"
            ),
            new CrawlTarget(
                    "club",
                    "동아리",
                    "https://linkareer.com/list/club?filterBy_interestIDs=13&filterType=INTEREST&orderBy_direction=DESC&orderBy_field=CREATED_AT&page=1"
            )
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<CrawlResponse> latestResult = new AtomicReference<>(
            new CrawlResponse(OffsetDateTime.now(KOREA_ZONE), 0, List.of(), "READY", null)
    );

    @Value("${app.crawl.schedule-enabled:false}")
    private boolean scheduleEnabled;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void crawlDaily() {
        if (scheduleEnabled) {
            refreshCrawlData();
        }
    }

    @GetMapping
    public CrawlResponse getCrawlData() {
        return latestResult.get();
    }

    @PostMapping("/refresh")
    public CrawlResponse refreshManually() {
        refreshCrawlData();
        return latestResult.get();
    }

    private synchronized void refreshCrawlData() {
        WebDriver driver = null;

        try {
            WebDriverManager.chromedriver().setup();
            driver = createDriver();

            List<CrawlItem> items = new ArrayList<>();

            for (CrawlTarget target : TARGETS) {
                Set<String> postLinks = collectLinksFromAllPages(driver, target.url());

                for (String detailUrl : postLinks) {
                    try {
                        items.add(crawlDetail(target, detailUrl));
                    } catch (Exception ignored) {
                        items.add(new CrawlItem(
                                target.type(),
                                target.name(),
                                "크롤링 실패",
                                detailUrl,
                                "",
                                "",
                                "",
                                Map.of(),
                                ""
                        ));
                    }
                }
            }

            latestResult.set(new CrawlResponse(
                    OffsetDateTime.now(KOREA_ZONE),
                    items.size(),
                    items,
                    "SUCCESS",
                    null
            ));
        } catch (Exception e) {
            latestResult.set(new CrawlResponse(
                    OffsetDateTime.now(KOREA_ZONE),
                    latestResult.get().count(),
                    latestResult.get().items(),
                    "FAILED",
                    e.getMessage()
            ));
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private CrawlItem crawlDetail(CrawlTarget target, String detailUrl) throws Exception {
        Document doc = Jsoup.connect(detailUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(15000)
                .get();

        JsonNode activity = extractActivityNode(doc);
        Map<String, String> basicInfo = extractBasicInfo(activity);

        return new CrawlItem(
                target.type(),
                target.name(),
                text(activity, "title"),
                detailUrl,
                text(activity.path("thumbnailImage"), "url"),
                text(activity, "organizationName"),
                extractDescription(doc),
                basicInfo,
                htmlToText(text(activity.path("detailText"), "text"))
        );
    }

    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1400,2200");
        options.addArguments("--lang=ko-KR");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
        return new ChromeDriver(options);
    }

    private Set<String> collectLinksFromAllPages(WebDriver driver, String firstPageUrl) throws InterruptedException {
        Set<String> allLinks = new LinkedHashSet<>();
        int page = 1;

        while (true) {
            String pageUrl = buildPageUrl(firstPageUrl, page);
            Set<String> pageLinks = collectLinksFromListPage(driver, pageUrl);
            int beforeSize = allLinks.size();

            allLinks.addAll(pageLinks);

            if (pageLinks.isEmpty() || allLinks.size() == beforeSize) {
                break;
            }

            page++;
        }

        return allLinks;
    }

    private String buildPageUrl(String firstPageUrl, int page) {
        if (firstPageUrl.matches(".*([?&])page=\\d+.*")) {
            return firstPageUrl.replaceAll("([?&]page=)\\d+", "$1" + page);
        }

        return firstPageUrl + (firstPageUrl.contains("?") ? "&" : "?") + "page=" + page;
    }

    private Set<String> collectLinksFromListPage(WebDriver driver, String listUrl) throws InterruptedException {
        driver.get(listUrl);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        Thread.sleep(3000);

        for (int i = 0; i < 2; i++) {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight * 0.6);");
            Thread.sleep(1500);
        }

        Set<String> postLinks = new LinkedHashSet<>();
        List<WebElement> links = driver.findElements(By.cssSelector("a[href*='/activity/']"));

        for (WebElement link : links) {
            String href = link.getAttribute("href");

            if (href != null && href.matches("https://linkareer\\.com/activity/\\d+.*")) {
                postLinks.add(href);
            }
        }

        return postLinks;
    }

    private JsonNode extractActivityNode(Document doc) throws Exception {
        Element nextData = doc.selectFirst("script#__NEXT_DATA__");
        if (nextData == null) {
            throw new IllegalStateException("__NEXT_DATA__ script not found");
        }

        JsonNode root = objectMapper.readTree(nextData.html());
        JsonNode activity = root.path("props").path("pageProps").path("data").path("activityData").path("activity");

        if (activity.isMissingNode() || activity.isNull()) {
            throw new IllegalStateException("activity data not found");
        }

        return activity;
    }

    private String extractDescription(Document doc) {
        Element ogDescription = doc.selectFirst("meta[property=og:description]");
        if (ogDescription != null && !ogDescription.attr("content").isBlank()) {
            return ogDescription.attr("content").trim();
        }

        Element description = doc.selectFirst("meta[name=description]");
        if (description != null) {
            return description.attr("content").trim();
        }

        return "";
    }

    private Map<String, String> extractBasicInfo(JsonNode activity) {
        Map<String, String> info = new LinkedHashMap<>();

        putIfNotBlank(info, "organizationType", text(activity, "organizationType"));
        putIfNotBlank(info, "targets", names(activity.path("targets")));
        putIfNotBlank(info, "recruitPeriod", period(activity, "recruitStartAt", "recruitCloseAt"));
        putIfNotBlank(info, "activityPeriod", period(activity, "activityStartAt", "activityEndAt"));
        putIfNotBlank(info, "recruitScale", withUnit(activity.path("integers"), "모집 인원"));
        putIfNotBlank(info, "regions", names(activity.path("regions")));
        putIfNotBlank(info, "interests", names(activity.path("interests")));
        putIfNotBlank(info, "categories", names(activity.path("categories")));
        putIfNotBlank(info, "benefits", names(activity.path("benefits")));
        putIfNotBlank(info, "skills", names(activity.path("skills")));
        putIfNotBlank(info, "homepageURL", text(activity, "homepageURL"));
        putIfNotBlank(info, "applyDetail", text(activity, "applyDetail"));

        return info;
    }

    private String period(JsonNode node, String startField, String endField) {
        String start = date(node.path(startField));
        String end = date(node.path(endField));

        if (start.isBlank() && end.isBlank()) {
            return "";
        }

        return start + " ~ " + end;
    }

    private String date(JsonNode node) {
        if (!node.isNumber()) {
            return "";
        }

        return Instant.ofEpochMilli(node.asLong())
                .atZone(KOREA_ZONE)
                .toLocalDate()
                .toString();
    }

    private String withUnit(JsonNode arrayNode, String typeName) {
        if (!arrayNode.isArray()) {
            return "";
        }

        for (JsonNode node : arrayNode) {
            JsonNode type = node.path("type");
            if (typeName.equals(text(type, "name"))) {
                String value = text(node, "integer");
                String unit = text(type, "unit");
                return value + unit;
            }
        }

        return "";
    }

    private String names(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return "";
        }

        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(node -> text(node, "name"))
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));
    }

    private void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String htmlToText(String html) {
        if (html.isBlank()) {
            return "";
        }

        return Jsoup.parse(html)
                .wholeText()
                .replace("\u00A0", " ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public record CrawlResponse(
            OffsetDateTime crawledAt,
            int count,
            List<CrawlItem> items,
            String status,
            String error
    ) {
    }

    public record CrawlItem(
            String category,
            String categoryName,
            String title,
            String url,
            String imageUrl,
            String organizationName,
            String description,
            Map<String, String> basicInfo,
            String detail
    ) {
    }

    private record CrawlTarget(
            String type,
            String name,
            String url
    ) {
    }
}
