package com.campusnavi.backend.official.crawler.parser.type;

import com.campusnavi.backend.official.crawler.dto.PostList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component("KU_SEJONG_RSS")
public class KuSejongRssParser extends KuSejongBasicParser {

    private static final DateTimeFormatter RSS_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    protected Document fetchRssDocument(String url) throws Exception {
        return Jsoup.connect(url)
                .parser(Parser.xmlParser())
                .userAgent("Mozilla/5.0")
                .timeout(15_000)
                .get();
    }

    @Override
    public List<PostList> fetchList(String rssUrl, int page) {
        try {
            String pagedUrl = appendPageParam(rssUrl, "page", page);
            Document rss = fetchRssDocument(pagedUrl);

            String baseUrl = extractOrigin(rssUrl);
            List<PostList> result = new ArrayList<>();
            for (Element item : rss.select("item")) {
                Element titleEl = item.selectFirst("title");
                Element linkEl  = item.selectFirst("link");
                if (titleEl == null || linkEl == null) continue;

                String title = titleEl.text().trim();
                String link  = resolveUrl(linkEl.text().trim(), baseUrl);

                Element pubDateEl = item.selectFirst("pubDate");
                LocalDate publishedAt = null;
                if (pubDateEl != null && !pubDateEl.text().isBlank()) {
                    publishedAt = LocalDateTime.parse(pubDateEl.text().trim(), RSS_DATE).toLocalDate();
                }

                Element authorEl = item.selectFirst("author");
                if (authorEl == null) authorEl = item.selectFirst("creator");
                String publisher = authorEl != null ? authorEl.text().trim() : null;

                result.add(new PostList(extractOriginalId(link), title, publisher, link, publishedAt));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("RSS 파싱 실패: " + rssUrl, e);
        }
    }
}
