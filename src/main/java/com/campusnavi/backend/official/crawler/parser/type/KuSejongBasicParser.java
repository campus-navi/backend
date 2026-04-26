package com.campusnavi.backend.official.crawler.parser.type;

import com.campusnavi.backend.official.crawler.dto.FileInfo;
import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.dto.PostDetail;
import com.campusnavi.backend.official.crawler.parser.AbstractJsoupParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Component("KU_SEJONG_BASIC")
public class KuSejongBasicParser extends AbstractJsoupParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Override
    public List<PostList> fetchList(String listUrl, int page) {
        try {
            String baseUrl = extractOrigin(listUrl);
            String pagedUrl = page == 1
                    ? listUrl
                    : appendPageParam(listUrl, "page", page);

            Document doc = fetchDocument(pagedUrl);
            List<PostList> result = new ArrayList<>();

            for (Element row : doc.select("tbody tr")) {
                Element a = row.selectFirst("td.td-title a");
                if (a == null) continue;

                String href = a.attr("href");
                String title = a.text().trim();

                Element publisherEl = row.selectFirst("td.td-write");
                String publisher = null;
                if (publisherEl != null && !publisherEl.text().isBlank()) {
                    publisher = publisherEl.text().trim();
                }

                Element dateEl = row.selectFirst("td.td-date");
                LocalDate publishedAt = null;
                if (dateEl != null && !dateEl.text().isBlank()) {
                    publishedAt = LocalDate.parse(dateEl.text().trim(), DATE_FORMATTER);
                }

                result.add(new PostList(extractOriginalId(href), title, publisher, baseUrl + href, publishedAt));
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("목록페이지 파싱 실패: " + listUrl, e);
        }
    }

    @Override
    public PostDetail fetchDetail(String detailUrl) {
        try {
            String baseUrl = extractOrigin(detailUrl);
            Document doc = fetchDocument(detailUrl);

            Element titleEl = doc.selectFirst(".title strong");
            String title = titleEl != null ? titleEl.text().trim() : "";
            Element contentEl = doc.selectFirst(".txt");
            String rawHtml = sanitizeHtml(contentEl, baseUrl);
            String plainText = contentEl != null ? contentEl.text() : "";

            List<FileInfo> images = extractImages(contentEl, baseUrl);
            List<FileInfo> attachments = extractAttachments(doc.body(), baseUrl);

            return new PostDetail(title, plainText, rawHtml, images, attachments);
        } catch (Exception e) {
            throw new RuntimeException("상세페이지 파싱 실패: " + detailUrl, e);
        }
    }

    private List<FileInfo> extractAttachments(Element attachEl, String baseUrl) {
        List<FileInfo> result = new ArrayList<>();
        if (attachEl == null) return result;

        Elements links = attachEl.select(".attachment a");
        for (Element a : links) {
            String href = a.attr("href");
            if (href.isBlank()) continue;
            String resolved = resolveUrl(href, baseUrl);
            String filename = a.text().trim();
            result.add(new FileInfo(filename, resolved, guessContentType(filename)));
        }
        return result;
    }

    protected String extractOriginalId(String href) {
        String path = href.split("\\?")[0];
        String[] segments = path.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i].matches("\\d+")) return segments[i];
        }
        return href;
    }
}
