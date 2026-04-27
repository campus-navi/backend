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

@Component("KU_SEOUL_BASIC")
public class KuSeoulBasicParser extends AbstractJsoupParser {


    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int PAGE_SIZE = 10;

    @Override
    public List<PostList> fetchList(String listUrl, int page) {
        try {
            String listPath = listUrl.contains("?")
                    ? listUrl.substring(0, listUrl.indexOf('?'))
                    : listUrl;

            int offset = (page - 1) * PAGE_SIZE;
            String pagedUrl = page == 1
                    ? listUrl
                    : appendPageParam(listUrl, "article.offset", offset);

            Document doc = fetchDocument(pagedUrl);
            List<PostList> result = new ArrayList<>();

            for (Element row : doc.select("tbody tr")) {
                Element a = row.selectFirst("td.txt_left a.article-title");
                if (a == null) continue;

                Elements tds = row.select("td");
                if (tds.size() < 5) continue;

                String href = a.attr("href");
                String title = a.text().trim();

                String publisher = tds.get(tds.size()-3).text().trim();
                String dateText = tds.getLast().text().trim();

                LocalDate publishedAt = null;
                if (!dateText.isBlank()) {
                    publishedAt = LocalDate.parse(dateText, DATE_FORMATTER);
                }
                result.add(new PostList(extractOriginalId(href), title, publisher, listPath + href, publishedAt));
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

            Element titleEl = doc.selectFirst("th:contains(제목) + td");
            String title = titleEl != null ? titleEl.text().trim() : "";

            Element contentEl = doc.selectFirst("div.article-text");
            String rawHtml = sanitizeHtml(contentEl, baseUrl);
            String structuredText = toStructuredText(contentEl);

            List<FileInfo> images = extractImages(contentEl, baseUrl);
            List<FileInfo> attachments = extractAttachments(doc, detailUrl);

            return new PostDetail(title, structuredText, rawHtml, images, attachments);
        } catch (Exception e) {
            throw new RuntimeException("상세페이지 파싱 실패: " + detailUrl, e);
        }
    }

    private List<FileInfo> extractAttachments(Document doc, String detailUrl) {
        String pagePath = detailUrl.contains("?") ? detailUrl.substring(0, detailUrl.indexOf('?')) : detailUrl;
        List<FileInfo> result = new ArrayList<>();
        for (Element a : doc.select("a.down[href]")) {
            String href = a.attr("href").trim();
            if (href.isBlank()) continue;
            String resolved = href.startsWith("?") ? pagePath + href : resolveUrl(href, extractOrigin(detailUrl));
            String filename = a.text().trim();
            result.add(new FileInfo(filename, resolved, guessContentType(filename)));
        }
        return result;
    }

    private String extractOriginalId(String href) {
        for (String param : href.split("[?&]")) {
            if (param.startsWith("articleNo=")) {
                return param.substring("articleNo=".length());
            }
        }
        return href;
    }
}
