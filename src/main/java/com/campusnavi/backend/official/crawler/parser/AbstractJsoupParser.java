package com.campusnavi.backend.official.crawler.parser;

import com.campusnavi.backend.official.crawler.dto.FileInfo;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class AbstractJsoupParser implements CrawlParser {

    protected Document fetchDocument(String url) throws IOException {
        try {
            return fetchLight(url);
        } catch (SocketTimeoutException | ConnectException e) {
            log.warn("1차 시도 실패({}), 헤더 강화 후 재시도 - url: {}", e.getClass().getSimpleName(), url);
            return fetchHeavy(url);
        } catch (UnknownHostException e) {
            log.error("DNS 해석 실패 - url: {}", url);
            throw e;
        } catch (SSLException e) {
            log.error("SSL 오류 - url: {}", url);
            throw e;
        } catch (HttpStatusException e) {
            log.error("HTTP {} 오류 - url: {}", e.getStatusCode(), url);
            throw e;
        }
    }

    private Document fetchLight(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .followRedirects(true)
                .get();
    }

    private Document fetchHeavy(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .referrer("https://www.google.com")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .timeout(30_000)
                .followRedirects(true)
                .get();
    }

    protected List<FileInfo> extractImages(Element contentEl, String baseUrl) {
        List<FileInfo> result = new ArrayList<>();
        if (contentEl == null) return result;

        Elements imgs = contentEl.select("img[src]");
        for (Element img : imgs) {
            String src = img.attr("src");
            String resolved = resolveUrl(src, baseUrl);
            String filename = resolved.substring(resolved.lastIndexOf('/') + 1);
            if (filename.contains("?")) filename = filename.substring(0, filename.indexOf('?'));
            result.add(new FileInfo(filename, resolved, guessContentType(filename)));
        }
        return result;
    }

    protected String resolveUrl(String url, String baseUrl) {
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("/")) return extractOrigin(baseUrl) + url;
        return baseUrl + "/" + url;
    }

    protected String extractOrigin(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        } catch (Exception e) {
            return url;
        }
    }

    protected String appendPageParam(String url, String paramName, int page) {
        return url + (url.contains("?") ? "&" : "?") + paramName + "=" + page;
    }

    protected String sanitizeHtml(Element element, String baseUrl) {
        if (element == null) return "";

        Element clone = element.clone();

        // 레이지 로딩 이미지: data-src → src 변환 (JS 미실행 환경 대응)
        for (Element img : clone.select("img[data-src]")) {
            img.attr("src", img.attr("data-src"));
            img.removeAttr("data-src");
        }

        // 상대 경로 src → 절대 URL 변환 (imageUrlMap 키 일치 보장)
        if (baseUrl != null) {
            for (Element img : clone.select("img[src]")) {
                String src = img.attr("src");
                if (!src.startsWith("http://") && !src.startsWith("https://")) {
                    img.attr("src", resolveUrl(src, baseUrl));
                }
            }
        }

        // 허용할 attribute만 남기고 나머지 전부 제거 (style, class, id, bgcolor, width 등)
        Set<String> keepAttrs = Set.of("href", "src", "alt");
        for (Element el : clone.getAllElements()) {
            List<String> toRemove = el.attributes().asList().stream()
                    .map(Attribute::getKey)
                    .filter(k -> !keepAttrs.contains(k))
                    .toList();
            toRemove.forEach(el::removeAttr);
        }

        // attribute 없는 bare <span> 언랩 (style/class 전용 span 제거)
        for (Element span : clone.select("span")) {
            if (span.attributes().isEmpty()) {
                span.unwrap();
            }
        }

        // HWP 에디터 전용 div 제거
        clone.select("div.hwp_editor_board_content").remove();

        String html = clone.html();
        html = html.replace("&nbsp;", " ");
        html = html.replace("\t", "");
        html = html.replaceAll("[ ]{2,}", " ");         // 연속 공백 → 단일 공백
        html = html.replaceAll("(?m)^ +$", "");         // 공백만 있는 줄 제거
        html = html.replaceAll("\\n{3,}", "\n\n");       // 3줄 이상 빈 줄 → 2줄

        return html.trim();
    }

    protected String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".hwp")) return "application/octet-stream";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "application/msword";
        return "application/octet-stream";
    }
}
