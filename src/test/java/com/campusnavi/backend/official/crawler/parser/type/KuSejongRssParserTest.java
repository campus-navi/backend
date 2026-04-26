package com.campusnavi.backend.official.crawler.parser.type;

import com.campusnavi.backend.official.crawler.dto.PostList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KuSejongRssParserTest {

    private TestParser parser;

    @BeforeEach
    void setUp() {
        parser = new TestParser();
    }

    static class TestParser extends KuSejongRssParser {
        private Document stubbedDoc;
        String capturedUrl;

        void stub(Document doc) { this.stubbedDoc = doc; }

        @Override
        protected Document fetchRssDocument(String url) {
            this.capturedUrl = url;
            return stubbedDoc;
        }
    }

    private static Document rss(String itemsXml) {
        String xml = "<rss><channel>" + itemsXml + "</channel></rss>";
        return Jsoup.parse(xml, "", Parser.xmlParser());
    }

    @Nested
    @DisplayName("목록 페이지 파싱")
    class FetchList {

        @Test
        @DisplayName("정상 RSS XML이면 게시물 목록을 파싱한다")
        void normalRss() {
            // given
            parser.stub(rss(
                    "<item>" +
                    "  <title>공지사항 제목</title>" +
                    "  <link>https://www.korea.ac.kr/notice/view/12345</link>" +
                    "  <pubDate>2024-01-15 09:00:00.0</pubDate>" +
                    "  <author>홍보팀</author>" +
                    "</item>"
            ));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(result).hasSize(1);
            PostList post = result.getFirst();
            assertThat(post.originalId()).isEqualTo("12345");
            assertThat(post.title()).isEqualTo("공지사항 제목");
            assertThat(post.detailUrl()).isEqualTo("https://www.korea.ac.kr/notice/view/12345");
            assertThat(post.publishedAt()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(post.publisher()).isEqualTo("홍보팀");
        }

        @Test
        @DisplayName("item이 없으면 빈 리스트를 반환한다")
        void emptyChannel() {
            // given
            parser.stub(rss(""));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("author 태그가 없으면 creator 태그로 publisher를 추출한다")
        void usesCreatorTag() {
            // given
            parser.stub(rss(
                    "<item>" +
                    "  <title>제목</title>" +
                    "  <link>https://www.korea.ac.kr/notice/view/1</link>" +
                    "  <pubDate>2024-01-15 09:00:00.0</pubDate>" +
                    "  <creator>학사팀</creator>" +
                    "</item>"
            ));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(result.getFirst().publisher()).isEqualTo("학사팀");
        }

        @Test
        @DisplayName("author와 creator 태그가 모두 없으면 publisher가 null이다")
        void noPublisher() {
            // given
            parser.stub(rss(
                    "<item>" +
                    "  <title>제목</title>" +
                    "  <link>https://www.korea.ac.kr/notice/view/1</link>" +
                    "  <pubDate>2024-01-15 09:00:00.0</pubDate>" +
                    "</item>"
            ));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(result.getFirst().publisher()).isNull();
        }

        @Test
        @DisplayName("pubDate가 비어있으면 publishedAt이 null이다")
        void emptyPubDate() {
            // given
            parser.stub(rss(
                    "<item>" +
                    "  <title>제목</title>" +
                    "  <link>https://www.korea.ac.kr/notice/view/1</link>" +
                    "  <pubDate></pubDate>" +
                    "</item>"
            ));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(result.getFirst().publishedAt()).isNull();
        }

        @Test
        @DisplayName("page에 관계없이 항상 page 파라미터를 URL에 추가한다")
        void alwaysAppendsPageParam() {
            // given
            parser.stub(rss(""));

            // when
            parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(parser.capturedUrl).isEqualTo("https://www.korea.ac.kr/rss.do?page=1");
        }

        @Test
        @DisplayName("title 태그가 없는 item이면 스킵한다")
        void missingTitle() {
            // given
            parser.stub(rss(
                    "<item>" +
                    "  <link>https://www.korea.ac.kr/notice/view/1</link>" +
                    "  <pubDate>2024-01-15 09:00:00.0</pubDate>" +
                    "</item>"
            ));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("link 태그가 없는 item이면 스킵한다")
        void missingLink() {
            // given
            parser.stub(rss(
                    "<item>" +
                    "  <title>제목</title>" +
                    "  <pubDate>2024-01-15 09:00:00.0</pubDate>" +
                    "</item>"
            ));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("유효한 item과 누락 item이 섞여있으면 유효한 item만 반환한다")
        void mixedItems() {
            // given
            parser.stub(rss(
                    "<item><title>정상1</title><link>https://www.korea.ac.kr/notice/view/1</link><pubDate>2024-01-15 09:00:00.0</pubDate></item>" +
                    "<item><link>https://www.korea.ac.kr/notice/view/2</link></item>" +
                    "<item><title>정상2</title><link>https://www.korea.ac.kr/notice/view/3</link><pubDate>2024-01-13 09:00:00.0</pubDate></item>"
            ));

            // when
            List<PostList> result = parser.fetchList("https://www.korea.ac.kr/rss.do", 1);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).title()).isEqualTo("정상1");
            assertThat(result.get(1).title()).isEqualTo("정상2");
        }
    }
}
