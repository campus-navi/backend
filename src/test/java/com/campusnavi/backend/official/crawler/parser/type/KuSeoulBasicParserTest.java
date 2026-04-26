package com.campusnavi.backend.official.crawler.parser.type;

import com.campusnavi.backend.official.crawler.dto.PostDetail;
import com.campusnavi.backend.official.crawler.dto.PostList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KuSeoulBasicParserTest {

    private TestParser parser;

    @BeforeEach
    void setUp() {
        parser = new TestParser();
    }

    static class TestParser extends KuSeoulBasicParser {
        private Document stubbedDoc;
        String capturedUrl;

        void stub(Document doc) { this.stubbedDoc = doc; }

        @Override
        protected Document fetchDocument(String url) {
            this.capturedUrl = url;
            return stubbedDoc;
        }
    }

    @Nested
    @DisplayName("목록 페이지 파싱")
    class FetchList {

        @Test
        @DisplayName("정상 HTML이면 articleNo를 originalId로 추출하여 게시물 목록을 파싱한다")
        void normalHtml() {
            // publisher = tds[size-3], date = tds[last]
            // given
            String html = "<table><tbody><tr>" +
                    "<td>1</td>" +
                    "<td class='txt_left'><a class='article-title' href='?articleNo=12345'>제목</a></td>" +
                    "<td>카테고리</td>" +
                    "<td>홍보팀</td>" +
                    "<td>100</td>" +
                    "<td>2024.01.15</td>" +
                    "</tr></tbody></table>";
            parser.stub(Jsoup.parse(html));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/notice.do?type=all", 1);

            // then
            assertThat(result).hasSize(1);
            PostList post = result.getFirst();
            assertThat(post.originalId()).isEqualTo("12345");
            assertThat(post.title()).isEqualTo("제목");
            assertThat(post.publisher()).isEqualTo("홍보팀");
            assertThat(post.publishedAt()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(post.detailUrl()).isEqualTo("https://www.korea.ac.kr/notice.do?articleNo=12345");
        }

        @Test
        @DisplayName("td 개수가 5 미만인 행이면 무시한다")
        void rowWithFewColumns() {
            // given
            String html = "<table><tbody><tr>" +
                    "<td class='txt_left'><a class='article-title' href='?articleNo=1'>제목</a></td>" +
                    "<td>a</td><td>b</td>" +
                    "</tr></tbody></table>";
            parser.stub(Jsoup.parse(html));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/notice.do", 1);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("tbody가 비어있으면 빈 리스트를 반환한다")
        void emptyTbody() {
            // given
            parser.stub(Jsoup.parse("<table><tbody></tbody></table>"));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/notice.do", 1);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("page가 1이면 URL을 변경하지 않는다")
        void page1() {
            // given
            parser.stub(Jsoup.parse("<table><tbody></tbody></table>"));

            // when
            parser.fetchList("https://www.korea.ac.kr/notice.do", 1);

            // then
            assertThat(parser.capturedUrl).isEqualTo("https://www.korea.ac.kr/notice.do");
        }

        @Test
        @DisplayName("page가 2 이상이면 article.offset 파라미터를 추가한다 (offset = (page-1) * 10)")
        void page3() {
            // given
            parser.stub(Jsoup.parse("<table><tbody></tbody></table>"));

            // when
            parser.fetchList("https://www.korea.ac.kr/notice.do", 3);

            // then
            assertThat(parser.capturedUrl).isEqualTo("https://www.korea.ac.kr/notice.do?article.offset=20");
        }

        @Test
        @DisplayName("listUrl에 쿼리스트링이 있으면 그 앞 경로에 href를 붙여 detailUrl을 만든다")
        void detailUrlFromListPath() {
            // given
            String html = "<table><tbody><tr>" +
                    "<td>1</td>" +
                    "<td class='txt_left'><a class='article-title' href='?articleNo=99'>제목</a></td>" +
                    "<td>분류</td><td>작성자</td><td>조회</td><td>2024.01.01</td>" +
                    "</tr></tbody></table>";
            parser.stub(Jsoup.parse(html));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/notice.do?type=all", 1);

            // then
            assertThat(result.getFirst().detailUrl())
                    .isEqualTo("https://www.korea.ac.kr/notice.do?articleNo=99");
        }
    }

    @Nested
    @DisplayName("상세 페이지 파싱")
    class FetchDetail {

        @Test
        @DisplayName("정상 HTML이면 제목과 본문을 파싱한다")
        void normalHtml() {
            // given
            String html = "<html><body>" +
                    "<table><tr><th>제목</th><td>공지사항 상세 제목</td></tr></table>" +
                    "<div class='article-text'><p>본문 내용입니다.</p></div>" +
                    "</body></html>";
            parser.stub(Jsoup.parse(html));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice.do?articleNo=100");

            // then
            assertThat(result.title()).isEqualTo("공지사항 상세 제목");
            assertThat(result.plainText()).contains("본문 내용입니다.");
        }

        @Test
        @DisplayName("div.article-text 내에 이미지가 있으면 FileInfo 목록으로 추출한다")
        void contentImages() {
            // given
            String html = "<html><body>" +
                    "<table><tr><th>제목</th><td>제목</td></tr></table>" +
                    "<div class='article-text'>" +
                    "<img src='https://www.korea.ac.kr/upload/photo.png'>" +
                    "</div>" +
                    "</body></html>";
            parser.stub(Jsoup.parse(html));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice.do?articleNo=100");

            // then
            assertThat(result.images()).hasSize(1);
            assertThat(result.images().getFirst().originalName()).isEqualTo("photo.png");
            assertThat(result.images().getFirst().contentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("a.down href가 ?로 시작하면 pagePath에 붙여 첨부파일 URL을 만든다")
        void attachmentWithQueryHref() {
            // given
            String html = "<html><body>" +
                    "<table><tr><th>제목</th><td>제목</td></tr></table>" +
                    "<div class='article-text'><p>내용</p></div>" +
                    "<a class='down' href='?fileNo=456'>첨부파일.hwp</a>" +
                    "</body></html>";
            parser.stub(Jsoup.parse(html));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice.do?articleNo=100");

            // then
            assertThat(result.attachments()).hasSize(1);
            assertThat(result.attachments().getFirst().originalName()).isEqualTo("첨부파일.hwp");
            assertThat(result.attachments().getFirst().originalUrl())
                    .isEqualTo("https://www.korea.ac.kr/notice.do?fileNo=456");
            assertThat(result.attachments().getFirst().contentType()).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("a.down href가 절대경로이면 그대로 사용한다")
        void attachmentWithAbsoluteHref() {
            // given
            String html = "<html><body>" +
                    "<table><tr><th>제목</th><td>제목</td></tr></table>" +
                    "<div class='article-text'><p>내용</p></div>" +
                    "<a class='down' href='https://files.korea.ac.kr/download/report.pdf'>보고서.pdf</a>" +
                    "</body></html>";
            parser.stub(Jsoup.parse(html));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice.do?articleNo=100");

            // then
            assertThat(result.attachments().getFirst().originalUrl())
                    .isEqualTo("https://files.korea.ac.kr/download/report.pdf");
        }

        @Test
        @DisplayName("title과 content 엘리먼트가 없으면 빈 문자열을 반환한다")
        void missingElements() {
            // given
            parser.stub(Jsoup.parse("<html><body></body></html>"));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice.do?articleNo=1");

            // then
            assertThat(result.title()).isEmpty();
            assertThat(result.plainText()).isEmpty();
            assertThat(result.images()).isEmpty();
            assertThat(result.attachments()).isEmpty();
        }
    }
}
