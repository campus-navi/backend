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

class KuSejongBasicParserTest {

    private TestParser parser;

    @BeforeEach
    void setUp() {
        parser = new TestParser();
    }

    static class TestParser extends KuSejongBasicParser {
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
        @DisplayName("정상 HTML이면 게시물 목록을 파싱한다")
        void normalHtml() {
            // given
            String html = "<table><tbody>" +
                    "<tr>" +
                    "  <td class='td-title'><a href='/notice/view/12345'>공지사항 제목</a></td>" +
                    "  <td class='td-write'>홍보팀</td>" +
                    "  <td class='td-date'>2024.01.15</td>" +
                    "</tr>" +
                    "</tbody></table>";
            parser.stub(Jsoup.parse(html));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/list.do", 1);

            // then
            assertThat(result).hasSize(1);
            PostList post = result.getFirst();
            assertThat(post.originalId()).isEqualTo("12345");
            assertThat(post.title()).isEqualTo("공지사항 제목");
            assertThat(post.publisher()).isEqualTo("홍보팀");
            assertThat(post.detailUrl()).isEqualTo("https://www.korea.ac.kr/notice/view/12345");
            assertThat(post.publishedAt()).isEqualTo(LocalDate.of(2024, 1, 15));
        }

        @Test
        @DisplayName("tbody가 비어있으면 빈 리스트를 반환한다")
        void emptyTbody() {
            // given
            parser.stub(Jsoup.parse("<table><tbody></tbody></table>"));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/list.do", 1);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("td.td-title a가 없는 행이면 무시한다")
        void rowWithoutAnchor() {
            // given
            String html = "<table><tbody>" +
                    "<tr><td class='td-title'>링크없음</td></tr>" +
                    "<tr><td class='td-title'><a href='/notice/view/1'>제목</a></td>" +
                    "    <td class='td-write'>작성자</td><td class='td-date'>2024.01.01</td></tr>" +
                    "</tbody></table>";
            parser.stub(Jsoup.parse(html));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/list.do", 1);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("page가 1이면 URL을 변경하지 않는다")
        void page1() {
            // given
            parser.stub(Jsoup.parse("<table><tbody></tbody></table>"));

            // when
            parser.fetchList("https://www.korea.ac.kr/list.do", 1);

            // then
            assertThat(parser.capturedUrl).isEqualTo("https://www.korea.ac.kr/list.do");
        }

        @Test
        @DisplayName("page가 2 이상이면 URL에 page 파라미터를 추가한다")
        void page2() {
            // given
            parser.stub(Jsoup.parse("<table><tbody></tbody></table>"));

            // when
            parser.fetchList("https://www.korea.ac.kr/list.do", 2);

            // then
            assertThat(parser.capturedUrl).isEqualTo("https://www.korea.ac.kr/list.do?page=2");
        }

        @Test
        @DisplayName("publisher와 publishedAt이 비어있으면 null로 처리한다")
        void emptyPublisherAndDate() {
            // given
            String html = "<table><tbody>" +
                    "<tr>" +
                    "  <td class='td-title'><a href='/notice/view/99'>제목</a></td>" +
                    "  <td class='td-write'></td>" +
                    "  <td class='td-date'></td>" +
                    "</tr>" +
                    "</tbody></table>";
            parser.stub(Jsoup.parse(html));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/list.do", 1);

            // then
            assertThat(result.getFirst().publisher()).isNull();
            assertThat(result.getFirst().publishedAt()).isNull();
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
                    "<div class='title'><strong>공지사항 상세 제목</strong></div>" +
                    "<div class='txt'><p>본문 내용입니다.</p></div>" +
                    "</body></html>";
            parser.stub(Jsoup.parse(html));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice/view/12345");

            // then
            assertThat(result.title()).isEqualTo("공지사항 상세 제목");
            assertThat(result.structuredText()).contains("본문 내용입니다.");
        }

        @Test
        @DisplayName("content 영역에 이미지가 있으면 FileInfo 목록으로 추출한다")
        void contentImages() {
            // given
            String html = "<html><body>" +
                    "<div class='title'><strong>제목</strong></div>" +
                    "<div class='txt'><img src='https://www.korea.ac.kr/upload/photo.jpg'></div>" +
                    "</body></html>";
            parser.stub(Jsoup.parse(html));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice/view/1");

            // then
            assertThat(result.images()).hasSize(1);
            assertThat(result.images().getFirst().originalName()).isEqualTo("photo.jpg");
            assertThat(result.images().getFirst().contentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName(".attachment 영역에 링크가 있으면 첨부파일로 추출하고 상대경로를 절대 URL로 변환한다")
        void attachmentLinks() {
            // given
            String html = "<html><body>" +
                    "<div class='title'><strong>제목</strong></div>" +
                    "<div class='txt'><p>내용</p></div>" +
                    "<div class='attachment'><a href='/file/download/456'>공지문서.pdf</a></div>" +
                    "</body></html>";
            parser.stub(Jsoup.parse(html));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice/view/1");

            // then
            assertThat(result.attachments()).hasSize(1);
            assertThat(result.attachments().getFirst().originalName()).isEqualTo("공지문서.pdf");
            assertThat(result.attachments().getFirst().contentType()).isEqualTo("application/pdf");
            assertThat(result.attachments().getFirst().originalUrl())
                    .isEqualTo("https://www.korea.ac.kr/file/download/456");
        }

        @Test
        @DisplayName("title과 content 엘리먼트가 없으면 빈 문자열을 반환한다")
        void missingElements() {
            // given
            parser.stub(Jsoup.parse("<html><body></body></html>"));

            // when
            PostDetail result = parser.fetchDetail("https://www.korea.ac.kr/notice/view/1");

            // then
            assertThat(result.title()).isEmpty();
            assertThat(result.structuredText()).isEmpty();
            assertThat(result.images()).isEmpty();
            assertThat(result.attachments()).isEmpty();
        }
    }
}
