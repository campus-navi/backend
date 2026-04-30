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
        @DisplayName("colspan, rowspan 속성은 유지된다")
        void tableSpanAttributes() {
            // given
            Document doc = Jsoup.parseBodyFragment(
                    "<div><table><tr><td colspan=\"3\" rowspan=\"2\">셀</td></tr></table></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.sanitizeHtml(el, null);

            // then
            assertThat(result).contains("colspan=\"3\"");
            assertThat(result).contains("rowspan=\"2\"");
        }

        @Test
        @DisplayName("colspan이 있어도 style 등 허용되지 않은 속성은 제거된다")
        void disallowedAttributesWithColspan() {
            // given
            Document doc = Jsoup.parseBodyFragment(
                    "<div><table><tr><td colspan=\"2\" style=\"width:100px\">셀</td></tr></table></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.sanitizeHtml(el, null);

            // then
            assertThat(result).contains("colspan=\"2\"");
            assertThat(result).doesNotContain("style=");
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
    @DisplayName("구조화 텍스트 변환")
    class ToStructuredText {

        @Test
        @DisplayName("element가 null이면 빈 문자열을 반환한다")
        void nullElement() {
            // when & then
            assertThat(parser.toStructuredText(null)).isEmpty();
        }

        @Test
        @DisplayName("p, div 태그이면 텍스트 뒤에 줄바꿈을 추가한다")
        void blockElements() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><p>첫 번째</p><p>두 번째</p></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).isEqualTo("첫 번째\n두 번째");
        }

        @Test
        @DisplayName("h1~h6 태그이면 텍스트 뒤에 줄바꿈을 추가한다")
        void headingElements() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><h2>제목</h2><p>내용</p></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).isEqualTo("제목\n내용");
        }

        @Test
        @DisplayName("br 태그이면 줄바꿈으로 변환한다")
        void brTag() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div>첫 줄<br>둘째 줄</div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).isEqualTo("첫 줄\n둘째 줄");
        }

        @Test
        @DisplayName("ul 태그이면 • 기호로 시작하는 목록으로 변환한다")
        void unorderedList() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><ul><li>항목 A</li><li>항목 B</li></ul></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).contains("• 항목 A");
            assertThat(result).contains("• 항목 B");
        }

        @Test
        @DisplayName("ol 태그이면 • 기호로 시작하는 목록으로 변환한다")
        void orderedList() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><ol><li>첫째</li><li>둘째</li></ol></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).contains("• 첫째");
            assertThat(result).contains("• 둘째");
        }

        @Test
        @DisplayName("table 태그이면 마크다운 표 형식으로 변환한다")
        void table() {
            // given
            Document doc = Jsoup.parseBodyFragment(
                    "<div><table><tr><th>이름</th><th>학번</th></tr><tr><td>홍길동</td><td>2024001</td></tr></table></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).contains("| 이름 | 학번 |");
            assertThat(result).contains("| 홍길동 | 2024001 |");
        }

        @Test
        @DisplayName("table 셀에 | 문자가 있으면 이스케이프 처리한다")
        void tablePipeEscape() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><table><tr><td>A|B</td></tr></table></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).contains("A\\|B");
        }

        @Test
        @DisplayName("모든 셀이 비어있는 tr이면 출력에서 제외한다")
        void emptyTableRow() {
            // given
            Document doc = Jsoup.parseBodyFragment(
                    "<div><table><tr><td></td><td></td></tr><tr><td>내용</td><td>값</td></tr></table></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result.lines().filter(l -> l.startsWith("|"))).hasSize(1);
            assertThat(result).contains("| 내용 | 값 |");
        }

        @Test
        @DisplayName("rowspan 셀은 해당 행마다 텍스트가 반복된다")
        void rowspan() {
            // given
            Document doc = Jsoup.parseBodyFragment(
                    "<div><table>" +
                    "<tr><td rowspan=\"2\">A</td><td>B</td></tr>" +
                    "<tr><td>C</td></tr>" +
                    "</table></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).contains("| A | B |");
            assertThat(result).contains("| A | C |");
        }

        @Test
        @DisplayName("colspan 셀은 해당 열마다 텍스트가 반복된다")
        void colspan() {
            // given
            Document doc = Jsoup.parseBodyFragment(
                    "<div><table>" +
                    "<tr><td colspan=\"2\">A</td><td>B</td></tr>" +
                    "<tr><td>C</td><td>D</td><td>E</td></tr>" +
                    "</table></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).contains("| A | A | B |");
            assertThat(result).contains("| C | D | E |");
        }

        @Test
        @DisplayName("rowspan + colspan 복합 표는 모든 행의 열 수가 동일하게 유지된다")
        void rowspanAndColspan() {
            // given
            Document doc = Jsoup.parseBodyFragment(
                    "<div><table>" +
                    "<tr><td rowspan=\"2\">수료요건</td><td colspan=\"2\">2020년</td><td>24학점</td></tr>" +
                    "<tr><td colspan=\"2\">2021년</td><td>30학점</td></tr>" +
                    "</table></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then — 두 행 모두 4칸(수료요건, colspan×2, 학점)
            assertThat(result).contains("| 수료요건 | 2020년 | 2020년 | 24학점 |");
            assertThat(result).contains("| 수료요건 | 2021년 | 2021년 | 30학점 |");
        }

        @Test
        @DisplayName("3줄 이상 연속 빈 줄이면 2줄로 압축한다")
        void collapseBlankLines() {
            // given
            Document doc = Jsoup.parseBodyFragment("<div><p>위</p><p></p><p></p><p>아래</p></div>");
            Element el = doc.body().selectFirst("div");

            // when
            String result = parser.toStructuredText(el);

            // then
            assertThat(result).doesNotContain("\n\n\n");
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
