package com.example.NextPlan.auth.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/crawl")
public class CrawlController {

    @PostMapping
    public Map<String, Object> crawl(@RequestBody CrawlRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = request.url();

        try {
            validateUrl(url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            String title = extractTitle(doc);
            Map<String, String> basicInfo = extractBasicInfo(doc);
            String detail = extractDetail(doc);
            String imageUrl = extractImageUrl(doc);

            result.put("success", true);
            result.put("requestedUrl", url);
            result.put("title", title);
            result.put("basicInfo", basicInfo);
            result.put("detail", detail);
            result.put("image", imageUrl);

        } catch (Exception e) {
            result.put("success", false);
            result.put("requestedUrl", url);
            result.put("message", e.getMessage());
        }

        return result;
    }

    private void validateUrl(String url) throws MalformedURLException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url은 필수입니다.");
        }

        URL parsedUrl = new URL(url);
        String protocol = parsedUrl.getProtocol();

        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new IllegalArgumentException("http 또는 https URL만 허용됩니다.");
        }
    }

    private String extractTitle(Document doc) {
        Element titleElement = doc.selectFirst("h1");
        return titleElement != null ? titleElement.text().trim() : "제목 없음";
    }

    private Map<String, String> extractBasicInfo(Document doc) {
        Map<String, String> basicInfo = new LinkedHashMap<>();

        for (Element dl : doc.select("dl")) {
            Element dt = dl.selectFirst("dt");
            Element dd = dl.selectFirst("dd");

            if (dt != null && dd != null) {
                basicInfo.put(dt.text().trim(), dd.text().trim());
            }
        }

        return basicInfo;
    }

    private String extractDetail(Document doc) {
        for (Element el : doc.getAllElements()) {
            if ("상세내용".equals(el.ownText().trim())) {
                Element parent = el.parent();
                Element next = parent != null ? parent.nextElementSibling() : null;

                if (next != null) {
                    StringBuilder sb = new StringBuilder();

                    for (Element p : next.select("p")) {
                        String text = p.text().trim();
                        if (!text.isEmpty()) {
                            sb.append(text).append("\n\n");
                        }
                    }

                    return sb.toString().trim();
                }
                break;
            }
        }

        return "";
    }

    private String extractImageUrl(Document doc) {
        Element img = doc.selectFirst("img.card-image");
        return img != null ? img.absUrl("src") : "";
    }

    public record CrawlRequest(String url) {
    }
}
