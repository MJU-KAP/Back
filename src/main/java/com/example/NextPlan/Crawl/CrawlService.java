package com.example.NextPlan.Crawl;

import com.example.NextPlan.Entity.ExternalActivity;
import com.example.NextPlan.Repository.ExternalActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            )
    );

    private final ExternalActivityRepository externalActivityRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrawlResult crawlAndSave() {
        try {
            log.info("Crawling started. targetCount={}", TARGETS.size());

            List<CrawledItem> items = new ArrayList<>();

            for (CrawlTarget target : TARGETS) {
                log.info("Collecting crawl links. category={}, firstPageUrl={}", target.category(), target.url());
                Set<String> detailUrls = collectLinksFromAllPages(target);
                log.info("Collected crawl links. category={}, count={}", target.category(), detailUrls.size());

                for (String detailUrl : detailUrls) {
                    try {
                        items.add(crawlDetail(target, detailUrl));
                    } catch (Exception e) {
                        log.warn("Failed to crawl detail page. url={}", detailUrl, e);
                    }
                }
            }

            int savedCount = saveCrawledItems(items);
            log.info("Crawling finished. crawledCount={}, savedCount={}", items.size(), savedCount);

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
        }
    }

    private int saveCrawledItems(List<CrawledItem> items) {
        int savedCount = 0;

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
            savedCount++;
        }

        return savedCount;
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

    private Set<String> collectLinksFromAllPages(CrawlTarget target) throws Exception {
        Set<String> allLinks = new LinkedHashSet<>();

        LinkareerListPage firstPage = collectLinksFromListPage(target, 1);
        int totalPages = calculateTotalPages(firstPage.totalCount(), firstPage.pageSize());
        log.info(
                "Linkareer list metadata. category={}, totalCount={}, pageSize={}, totalPages={}",
                target.category(),
                firstPage.totalCount(),
                firstPage.pageSize(),
                totalPages
        );

        addPageLinks(target, allLinks, firstPage);

        for (int page = 2; page <= totalPages; page++) {
            LinkareerListPage currentPage = collectLinksFromListPage(target, page);
            addPageLinks(target, allLinks, currentPage);
        }

        return allLinks;
    }

    private void addPageLinks(CrawlTarget target, Set<String> allLinks, LinkareerListPage pageData) {
        int beforeSize = allLinks.size();
        allLinks.addAll(pageData.links());
        int newLinkCount = allLinks.size() - beforeSize;

        log.info(
                "Collected links from page. category={}, page={}, url={}, pageLinkCount={}, newLinkCount={}",
                target.category(),
                pageData.page(),
                pageData.url(),
                pageData.links().size(),
                newLinkCount
        );
    }

    private String buildPageUrl(String firstPageUrl, int page) {
        if (firstPageUrl.matches(".*([?&])page=\\d+.*")) {
            return firstPageUrl.replaceAll("([?&]page=)\\d+", "$1" + page);
        }

        return firstPageUrl + (firstPageUrl.contains("?") ? "&" : "?") + "page=" + page;
    }

    private LinkareerListPage collectLinksFromListPage(CrawlTarget target, int page) throws Exception {
        String listUrl = buildPageUrl(target.url(), page);
        Document doc = Jsoup.connect(listUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .timeout(15000)
                .get();

        JsonNode root = extractNextData(doc);
        ActivityConnection connection = extractActivityConnection(root, page);
        Set<String> postLinks = extractActivityLinks(connection.nodes());

        return new LinkareerListPage(
                page,
                listUrl,
                connection.totalCount(),
                connection.pageSize(),
                postLinks
        );
    }

    private ActivityConnection extractActivityConnection(JsonNode root, int page) {
        JsonNode rootQuery = root.path("props")
                .path("pageProps")
                .path("__APOLLO_STATE__")
                .path("ROOT_QUERY");

        Iterator<Map.Entry<String, JsonNode>> fields = rootQuery.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();

            if (key.startsWith("activities(") && parseIntFromQueryKey(key, "page", -1) == page) {
                JsonNode connection = field.getValue();
                int pageSize = parseIntFromQueryKey(key, "pageSize", connection.path("nodes").size());

                return new ActivityConnection(
                        connection.path("totalCount").asInt(0),
                        pageSize,
                        connection.path("nodes")
                );
            }
        }

        throw new IllegalStateException("activities query not found. page=" + page);
    }

    private int parseIntFromQueryKey(String key, String fieldName, int defaultValue) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(fieldName) + "\\\":(\\d+)");
        Matcher matcher = pattern.matcher(key);

        return matcher.find() ? Integer.parseInt(matcher.group(1)) : defaultValue;
    }

    private int calculateTotalPages(int totalCount, int pageSize) {
        if (totalCount <= 0 || pageSize <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalCount / pageSize);
    }

    private Set<String> extractActivityLinks(JsonNode nodes) {
        Set<String> postLinks = new LinkedHashSet<>();

        if (!nodes.isArray()) {
            return postLinks;
        }

        for (JsonNode node : nodes) {
            String ref = text(node, "__ref");

            if (ref.startsWith("Activity:")) {
                postLinks.add("https://linkareer.com/activity/" + ref.substring("Activity:".length()));
            }
        }

        return postLinks;
    }

    private JsonNode extractActivityNode(Document doc) throws Exception {
        JsonNode root = extractNextData(doc);
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

    private JsonNode extractNextData(Document doc) throws Exception {
        Element nextData = doc.selectFirst("script#__NEXT_DATA__");

        if (nextData == null) {
            throw new IllegalStateException("__NEXT_DATA__ script not found");
        }

        return objectMapper.readTree(nextData.html());
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

    private record LinkareerListPage(
            int page,
            String url,
            int totalCount,
            int pageSize,
            Set<String> links
    ) {
    }

    private record ActivityConnection(
            int totalCount,
            int pageSize,
            JsonNode nodes
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
