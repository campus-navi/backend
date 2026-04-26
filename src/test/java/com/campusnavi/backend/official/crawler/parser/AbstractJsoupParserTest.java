package com.campusnavi.backend.official.crawler.parser;

import com.campusnavi.backend.official.crawler.dto.FileInfo;
import com.campusnavi.backend.official.crawler.dto.PostDetail;
import com.campusnavi.backend.official.crawler.dto.PostList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractJsoupParserTest {

    private final AbstractJsoupParser parser = new AbstractJsoupParser() {
        @Override public List<PostList> fetchList(String listUrl, int page) { return List.of(); }
        @Override public PostDetail fetchDetail(String detailUrl) { return null; }
    };

    @Nested
    @DisplayName("URL 경로 변환")
    class ResolveUrl {

        @Test
        @DisplayName("http/https로 시작하는 절대 URL이면 그대로 반환한다")
        void absoluteUrl() {
            // when & then
            assertThat(parser.resolveUrl("https://example.com/img.png", "https://base.com"))
                    .isEqualTo("https://example.com/img.png");
            assertThat(parser.resolveUrl("http://example.com/img.png", "https://base.com"))
                    .isEqualTo("http://example.com/img.png");
        }

        @Test
        @DisplayName("//로 시작하면 https: 를 붙여 반환한다")
        void protocolRelative() {
            // when & then
            assertThat(parser.resolveUrl("//cdn.example.com/img.png", "https://base.com"))
                    .isEqualTo("https://cdn.example.com/img.png");
        }

        @Test
        @DisplayName("/로 시작하는 루트 상대경로이면 origin에 붙여 반환한다")
        void rootRelative() {
            // when & then
            assertThat(parser.resolveUrl("/notice/view.do", "https://www.korea.ac.kr/list.do"))
                    .isEqualTo("https://www.korea.ac.kr/notice/view.do");
        }

        @Test
        @DisplayName("그 외 상대경로이면 baseUrl 뒤에 붙여 반환한다")
        void relativeUrl() {
            // when & then
            assertThat(parser.resolveUrl("img/photo.jpg", "https://example.com/board"))
                    .isEqualTo("https://example.com/board/img/photo.jpg");
        }
    }

    @Nested
    @DisplayName("MIME 타입 추론")
    class GuessContentType {

        @Test
        @DisplayName("이미지 확장자이면 image/* MIME을 반환한다")
        void imageExtensions() {
            // when & then
            assertThat(parser.guessContentType("photo.jpg")).isEqualTo("image/jpeg");
            assertThat(parser.guessContentType("photo.jpeg")).isEqualTo("image/jpeg");
            assertThat(parser.guessContentType("logo.PNG")).isEqualTo("image/png");
            assertThat(parser.guessContentType("anim.gif")).isEqualTo("image/gif");
            assertThat(parser.guessContentType("banner.webp")).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("문서 확장자이면 application/* MIME을 반환한다")
        void documentExtensions() {
            // when & then
            assertThat(parser.guessContentType("report.pdf")).isEqualTo("application/pdf");
            assertThat(parser.guessContentType("report.hwp")).isEqualTo("application/octet-stream");
            assertThat(parser.guessContentType("data.xlsx")).isEqualTo("application/vnd.ms-excel");
            assertThat(parser.guessContentType("data.xls")).isEqualTo("application/vnd.ms-excel");
            assertThat(parser.guessContentType("doc.docx")).isEqualTo("application/msword");
            assertThat(parser.guessContentType("doc.doc")).isEqualTo("application/msword");
        }

        @Test
        @DisplayName("알 수 없는 확장자이면 application/octet-stream을 반환한다")
        void unknownExtension() {
            // when & then
            assertThat(parser.guessContentType("archive.zip")).isEqualTo("application/octet-stream");
        }
    }

    @Nested
    @DisplayName("HTML 정제")
    class SanitizeHtml {

        @Test
        @DisplayName("element가 null이면 빈 문자열을 반환한다")
        void nullElement() {
            // when & then
            assertThat(parser.sanitizeHtml(null, "https://example.com")).isEmpty();
        }

        @Test
        @DisplayName("data-src를 src로 변환하고 상대경로이면 절대 URL로 만든다")
        void dataSrc() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><img data-src='/img/photo.jpg' alt='test'></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.sanitizeHtml(el, "https://example.com");

            // then
            assertThat(result).contains("src=\"https://example.com/img/photo.jpg\"");
            assertThat(result).doesNotContain("data-src");
        }

        @Test
        @DisplayName("상대경로인 img src이면 절대 URL로 변환한다")
        void relativeImgSrc() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><img src='/upload/img.png'></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.sanitizeHtml(el, "https://example.com");

            // then
            assertThat(result).contains("src=\"https://example.com/upload/img.png\"");
        }

        @Test
        @DisplayName("style, class 등 허용되지 않은 attribute이면 제거한다")
        void disallowedAttributes() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><p style='color:red' class='content'>text</p></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.sanitizeHtml(el, null);

            // then
            assertThat(result).doesNotContain("style=");
            assertThat(result).doesNotContain("class=");
            assertThat(result).contains("text");
        }

        @Test
        @DisplayName("href, src, alt이면 그대로 유지한다")
        void allowedAttributes() {
            // given
            Document doc = Jsoup.parseBodyFragment(
                    "<div><a href='https://example.com'>link</a><img src='https://example.com/img.png' alt='이미지'></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.sanitizeHtml(el, null);

            // then
            assertThat(result).contains("href=\"https://example.com\"");
            assertThat(result).contains("alt=\"이미지\"");
        }

        @Test
        @DisplayName("attribute 없는 span이면 언랩하여 텍스트만 남긴다")
        void bareSpan() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><span>내용</span></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.sanitizeHtml(el, null);

            // then
            assertThat(result).doesNotContain("<span");
            assertThat(result).contains("내용");
        }

        @Test
        @DisplayName("&nbsp;이면 공백으로 치환한다")
        void nbsp() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div>hello&nbsp;world</div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.sanitizeHtml(el, null);

            // then
            assertThat(result).contains("hello world");
        }
    }

    @Nested
    @DisplayName("페이지 파라미터 추가")
    class AppendPageParam {

        @Test
        @DisplayName("쿼리스트링이 없는 URL이면 ?param=page를 추가한다")
        void noQueryString() {
            // when & then
            assertThat(parser.appendPageParam("https://example.com/list", "page", 2))
                    .isEqualTo("https://example.com/list?page=2");
        }

        @Test
        @DisplayName("쿼리스트링이 있는 URL이면 &param=page를 추가한다")
        void existingQueryString() {
            // when & then
            assertThat(parser.appendPageParam("https://example.com/list?type=notice", "page", 2))
                    .isEqualTo("https://example.com/list?type=notice&page=2");
        }
    }

    @Nested
    @DisplayName("이미지 추출")
    class ExtractImages {

        @Test
        @DisplayName("element가 null이면 빈 리스트를 반환한다")
        void nullElement() {
            // when & then
            assertThat(parser.extractImages(null, "https://example.com")).isEmpty();
        }

        @Test
        @DisplayName("img[src] 태그이면 FileInfo 목록으로 추출한다")
        void imgTags() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><img src='https://example.com/img/photo.jpg'></div>");
            Element el = doc.body().selectFirst("div");

            // when
            List<FileInfo> images = parser.extractImages(el, "https://example.com");

            // then
            assertThat(images).hasSize(1);
            assertThat(images.getFirst().originalName()).isEqualTo("photo.jpg");
            assertThat(images.getFirst().originalUrl()).isEqualTo("https://example.com/img/photo.jpg");
            assertThat(images.getFirst().contentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("src에 쿼리스트링이 있으면 파일명에서 제거한다")
        void queryStringInSrc() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><img src='https://example.com/img.png?v=1'></div>");
            Element el = doc.body().selectFirst("div");

            // when
            List<FileInfo> images = parser.extractImages(el, "https://example.com");

            // then
            assertThat(images.getFirst().originalName()).isEqualTo("img.png");
        }
    }
}
