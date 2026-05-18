package com.example.NextPlan.Crawl;

import com.example.NextPlan.Entity.ExternalActivity;
import com.example.NextPlan.Repository.ExternalActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private static final List<CrawlTarget> TARGETS = List.of(
            new CrawlTarget(
                    "activity",
                    "https://linkareer.com/list/activity?filterBy_interestIDs=13&filterType=INTEREST&orderBy_direction=DESC&orderBy_field=CREATED_AT&page=1"
            ),
            new CrawlTarget(
                    "contest",
                    "https://linkareer.com/list/contest?filterBy_categoryIDs=35&filterBy_categoryIDs=41&filterType=CATEGORY&orderBy_direction=DESC&orderBy_field=CREATED_AT&page=1"
            ),
            new CrawlTarget(
                    "club",
                    "https://linkareer.com/list/club?filterBy_interestIDs=13&filterType=INTEREST&orderBy_direction=DESC&orderBy_field=CREATED_AT&page=1"
            )
    );

    private final ExternalActivityRepository externalActivityRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrawlResult crawlAndSave() {
        WebDriver driver = null;

        try {
            WebDriverManager.chromedriver().setup();
            driver = createDriver();

            List<CrawledItem> items = new ArrayList<>();

            for (CrawlTarget target : TARGETS) {
                Set<String> detailUrls = collectLinksFromAllPages(driver, target.url());

                for (String detailUrl : detailUrls) {
                    try {
                        items.add(crawlDetail(target, detailUrl));
                    } catch (Exception e) {
                        log.warn("Failed to crawl detail page. url={}", detailUrl, e);
                    }
                }
            }

            saveCrawledItems(items);

            return new CrawlResult(
                    OffsetDateTime.now(KOREA_ZONE),
                    items.size(),
                    "SUCCESS",
                    null
            );
        } catch (Exception e) {
            log.error("Crawling failed", e);

            return new CrawlResult(
                    OffsetDateTime.now(KOREA_ZONE),
                    0,
                    "FAILED",
                    e.getMessage()
            );
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private void saveCrawledItems(List<CrawledItem> items) {
        for (CrawledItem item : items) {
            if (item.originUrl() == null || item.originUrl().isBlank()) {
                continue;
            }

            ExternalActivity activity = externalActivityRepository.findByOriginUrl(item.originUrl())
                    .map(existing -> {
                        existing.update(
                                item.title(),
                                item.category(),
                                item.requiredSkills(),
                                item.recruitEndDate(),
                                item.originUrl(),
                                item.homepageUrl(),
                                item.companyType(),
                                item.targetAudience(),
                                item.recruitStartDate(),
                                item.imageUri(),
                                item.activityBenefit(),
                                item.extraBenefit(),
                                item.activityStartDate(),
                                item.activityEndDate(),
                                item.recruitCount(),
                                item.activityRegion(),
                                item.interestField(),
                                item.awardScale(),
                                item.contestField(),
                                item.detail()
                        );
                        return existing;
                    })
                    .orElseGet(() -> ExternalActivity.builder()
                            .title(item.title())
                            .category(item.category())
                            .requiredSkills(item.requiredSkills())
                            .recruitEndDate(item.recruitEndDate())
                            .originUrl(item.originUrl())
                            .homepageUrl(item.homepageUrl())
                            .companyType(item.companyType())
                            .targetAudience(item.targetAudience())
                            .recruitStartDate(item.recruitStartDate())
                            .imageUri(item.imageUri())
                            .activityBenefit(item.activityBenefit())
                            .extraBenefit(item.extraBenefit())
                            .activityStartDate(item.activityStartDate())
                            .activityEndDate(item.activityEndDate())
                            .recruitCount(item.recruitCount())
                            .activityRegion(item.activityRegion())
                            .interestField(item.interestField())
                            .awardScale(item.awardScale())
                            .contestField(item.contestField())
                            .detail(item.detail())
                            .build());

            externalActivityRepository.save(activity);
        }
    }

    private CrawledItem crawlDetail(CrawlTarget target, String detailUrl) throws Exception {
        Document doc = Jsoup.connect(detailUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .timeout(15000)
                .get();

        JsonNode activity = extractActivityNode(doc);

        return new CrawledItem(
                target.category(),
                text(activity, "title"),
                detailUrl,
                text(activity, "homepageURL"),
                nameOrText(activity.path("organizationType")),
                names(activity.path("targets")),
                date(activity.path("recruitStartAt")),
                date(activity.path("recruitCloseAt")),
                text(activity.path("thumbnailImage"), "url"),
                names(activity.path("benefits")),
                text(activity, "applyDetail"),
                date(activity.path("activityStartAt")),
                date(activity.path("activityEndAt")),
                integerByTypeName(activity.path("integers"), "모집"),
                names(activity.path("regions")),
                names(activity.path("interests")),
                monetaryByTypeName(activity.path("monetaries")),
                names(activity.path("categories")),
                skills(activity.path("skills")),
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
        JsonNode activity = root.path("props")
                .path("pageProps")
                .path("data")
                .path("activityData")
                .path("activity");

        if (activity.isMissingNode() || activity.isNull()) {
            throw new IllegalStateException("activity data not found");
        }

        return activity;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String nameOrText(JsonNode node) {
        if (node.isTextual()) {
            return node.asText("");
        }

        return text(node, "name");
    }

    private String names(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return "";
        }

        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(this::nameOrText)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private List<String> skills(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return List.of();
        }

        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(this::nameOrText)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private LocalDate date(JsonNode node) {
        if (!node.isNumber()) {
            return null;
        }

        return Instant.ofEpochMilli(node.asLong())
                .atZone(KOREA_ZONE)
                .toLocalDate();
    }

    private Integer integerByTypeName(JsonNode arrayNode, String keyword) {
        if (!arrayNode.isArray()) {
            return null;
        }

        for (JsonNode node : arrayNode) {
            String typeName = text(node.path("type"), "name");

            if (typeName.contains(keyword)) {
                JsonNode value = node.path("integer");
                return value.isNumber() ? value.asInt() : null;
            }
        }

        return null;
    }

    private String monetaryByTypeName(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return "";
        }

        for (JsonNode node : arrayNode) {
            String amount = text(node, "amount");
            String unit = text(node.path("type"), "unit");

            if (!amount.isBlank()) {
                return amount + unit;
            }
        }

        return "";
    }

    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        return Jsoup.parse(html)
                .wholeText()
                .replace("\u00A0", " ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public record CrawlResult(
            OffsetDateTime crawledAt,
            int count,
            String status,
            String error
    ) {
    }

    private record CrawlTarget(
            String category,
            String url
    ) {
    }

    private record CrawledItem(
            String category,
            String title,
            String originUrl,
            String homepageUrl,
            String companyType,
            String targetAudience,
            LocalDate recruitStartDate,
            LocalDate recruitEndDate,
            String imageUri,
            String activityBenefit,
            String extraBenefit,
            LocalDate activityStartDate,
            LocalDate activityEndDate,
            Integer recruitCount,
            String activityRegion,
            String interestField,
            String awardScale,
            String contestField,
            List<String> requiredSkills,
            String detail
    ) {
    }
    @Transactional(readOnly = true)
    public CrawlResponse getCrawlData(String category) {
        List<ExternalActivity> activities =
                category == null || category.isBlank()
                        ? externalActivityRepository.findAllByOrderByExtIdDesc()
                        : externalActivityRepository.findByCategoryOrderByExtIdDesc(category);

        List<ActivityResponse> items = activities.stream()
                .map(this::toActivityResponse)
                .toList();

        return new CrawlResponse(items.size(), items);
    }

    private ActivityResponse toActivityResponse(ExternalActivity activity) {
        return new ActivityResponse(
                activity.getExtId(),
                activity.getCategory(),
                activity.getTitle(),
                activity.getOriginUrl(),
                activity.getHomepageUrl(),
                activity.getCompanyType(),
                activity.getTargetAudience(),
                activity.getRecruitStartDate(),
                activity.getRecruitEndDate(),
                activity.getImageUri(),
                activity.getActivityBenefit(),
                activity.getExtraBenefit(),
                activity.getActivityStartDate(),
                activity.getActivityEndDate(),
                activity.getRecruitCount(),
                activity.getActivityRegion(),
                activity.getInterestField(),
                activity.getAwardScale(),
                activity.getContestField(),
                activity.getRequiredSkills(),
                activity.getDetail()
        );
    }

    public record CrawlResponse(
            int count,
            List<ActivityResponse> items
    ) {
    }

    public record ActivityResponse(
            Integer extId,
            String category,
            String title,
            String originUrl,
            String homepageUrl,
            String companyType,
            String targetAudience,
            LocalDate recruitStartDate,
            LocalDate recruitEndDate,
            String imageUri,
            String activityBenefit,
            String extraBenefit,
            LocalDate activityStartDate,
            LocalDate activityEndDate,
            Integer recruitCount,
            String activityRegion,
            String interestField,
            String awardScale,
            String contestField,
            List<String> requiredSkills,
            String result
    ) {
    }

}
