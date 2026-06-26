package com.campusnavi.backend.official.post.service;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.member.repository.MemberQueryRepository;
import com.campusnavi.backend.official.post.dto.OfficialPostDetailResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostListSort;
import com.campusnavi.backend.official.post.dto.OfficialPostSummaryRaw;
import com.campusnavi.backend.official.post.dto.OfficialPostSummaryResponse;
import com.campusnavi.backend.official.post.entity.ApplyMethodType;
import com.campusnavi.backend.official.post.entity.OfficialAttachment;
import com.campusnavi.backend.official.post.entity.OfficialPost;
import com.campusnavi.backend.official.post.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.post.repository.OfficialAttachmentDownloadRepository;
import com.campusnavi.backend.official.post.repository.OfficialAttachmentRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostAiMetaRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostNotificationRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostQueryRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostRepository;
import com.campusnavi.backend.official.post.repository.OfficialPostScrapRepository;
import com.campusnavi.backend.tag.entity.Tag;
import com.campusnavi.backend.tag.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OfficialPostServiceTest {

    @Mock
    private OfficialPostRepository postRepository;

    @Mock
    private OfficialPostAiMetaRepository aiMetaRepository;

    @Mock
    private OfficialAttachmentRepository attachmentRepository;

    @Mock
    private OfficialAttachmentDownloadRepository downloadRepository;

    @Mock
    private OfficialPostScrapRepository scrapRepository;

    @Mock
    private OfficialPostNotificationRepository notificationRepository;

    @Mock
    private OfficialPostViewService viewService;

    @Mock
    private S3StorageService storageService;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private OfficialPostQueryRepository officialPostQueryRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private OfficialPostService officialPostService;

    private static final Long POST_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);

    @Nested
    @DisplayName("공식 공지 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("정상 요청이면 공지/AI메타/첨부 정보를 합쳐 응답한다")
        void success() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment image = mockAttachment(900L, "img.png", "img/a.png", true);
            OfficialAttachment file = mockAttachment(910L, "doc.pdf", "file/b.pdf", false);

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(image, file));
            given(storageService.resolveUrl("img/a.png")).willReturn("https://cdn/img/a.png");
            given(downloadRepository.findDownloadedAttachmentIds(MEMBER_ID, List.of(910L)))
                    .willReturn(Set.of());
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.postId()).isEqualTo(POST_ID);
            assertThat(response.title()).isEqualTo("제목");
            assertThat(response.publisher()).isEqualTo("학사팀");
            assertThat(response.tagName()).isEqualTo("장학금");
            assertThat(response.summary()).isEqualTo("요약");
            assertThat(response.contentHtml()).isEqualTo("<p>본문</p>");
            assertThat(response.isApplicable()).isTrue();
            assertThat(response.applyMethodType()).isEqualTo(ApplyMethodType.FILE);
            assertThat(response.imageUrls()).containsExactly("https://cdn/img/a.png");
            assertThat(response.attachments()).hasSize(1);
            assertThat(response.attachments().getFirst().id()).isEqualTo(910L);
            assertThat(response.attachments().getFirst().name()).isEqualTo("doc.pdf");
            assertThat(response.attachments().getFirst().isDownloaded()).isFalse();
            assertThat(response.hasUnreadAttachments()).isTrue();
            assertThat(response.isScrapped()).isFalse();
            assertThat(response.isNotificationOn()).isFalse();
            then(viewService).should().recordView(MEMBER_ID, POST_ID);
        }

        @Test
        @DisplayName("스크랩된 공지를 조회하면 isScrapped가 true이다")
        void scrappedTrue() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).willReturn(List.of());
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(true);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.isScrapped()).isTrue();
        }

        @Test
        @DisplayName("알림이 켜진 공지를 조회하면 isNotificationOn이 true이다")
        void notificationOnTrue() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE)).willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).willReturn(List.of());
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(true);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.isNotificationOn()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화/스코프 밖 공지이면 OFFICIAL_POST_NOT_FOUND 예외가 발생한다")
        void postNotFound() {
            // given
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostService.getDetail(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_FOUND));
        }

        @Test
        @DisplayName("AI 후처리가 완료되지 않았으면 OFFICIAL_POST_NOT_READY 예외가 발생한다")
        void metaNotReady() {
            // given
            OfficialPost post = mockPost();
            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> officialPostService.getDetail(POST_ID, CONTEXT))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_POST_NOT_READY));
        }

        @Test
        @DisplayName("이미지 첨부가 없으면 imageUrls는 빈 리스트이다")
        void noImageAttachment() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment file = mockAttachment(910L, "doc.pdf", "file/b.pdf", false);

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(file));
            given(downloadRepository.findDownloadedAttachmentIds(MEMBER_ID, List.of(910L)))
                    .willReturn(Set.of());
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.imageUrls()).isEmpty();
            assertThat(response.attachments()).hasSize(1);
        }

        @Test
        @DisplayName("비이미지 첨부가 없으면 attachments는 빈 리스트이고 hasUnreadAttachments는 false이며 다운로드 이력 조회도 호출되지 않는다")
        void noFileAttachment() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment image = mockAttachment(900L, "img.png", "img/a.png", true);

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(image));
            given(storageService.resolveUrl("img/a.png")).willReturn("https://cdn/img/a.png");
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.imageUrls()).containsExactly("https://cdn/img/a.png");
            assertThat(response.attachments()).isEmpty();
            assertThat(response.hasUnreadAttachments()).isFalse();
            then(downloadRepository).should(never()).findDownloadedAttachmentIds(any(), anyList());
        }

        @Test
        @DisplayName("첨부파일이 전혀 없으면 imageUrls와 attachments는 모두 빈 리스트이며 hasUnreadAttachments는 false이다")
        void noAttachment() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).willReturn(List.of());
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.imageUrls()).isEmpty();
            assertThat(response.attachments()).isEmpty();
            assertThat(response.hasUnreadAttachments()).isFalse();
            then(downloadRepository).should(never()).findDownloadedAttachmentIds(any(), anyList());
        }

        @Test
        @DisplayName("이미지 첨부가 여러 개이면 imageUrls가 sortOrder 순서대로 모두 담긴다")
        void multipleImageAttachments() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment image1 = mockAttachment(900L, "img1.png", "img/a.png", true);
            OfficialAttachment image2 = mockAttachment(901L, "img2.png", "img/b.png", true);
            OfficialAttachment file = mockAttachment(910L, "doc.pdf", "file/c.pdf", false);

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(image1, image2, file));
            given(storageService.resolveUrl("img/a.png")).willReturn("https://cdn/img/a.png");
            given(storageService.resolveUrl("img/b.png")).willReturn("https://cdn/img/b.png");
            given(downloadRepository.findDownloadedAttachmentIds(MEMBER_ID, List.of(910L)))
                    .willReturn(Set.of());
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.imageUrls())
                    .containsExactly("https://cdn/img/a.png", "https://cdn/img/b.png");
            assertThat(response.attachments()).hasSize(1);
            assertThat(response.attachments().getFirst().id()).isEqualTo(910L);
        }

        @Test
        @DisplayName("AI 메타에 tag가 null이면 tagName도 null이다")
        void nullTag() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(null);

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).willReturn(List.of());
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.tagName()).isNull();
        }

        @Test
        @DisplayName("일부 첨부에만 다운로드 이력이 있으면 해당 항목만 isDownloaded=true이고 나머지는 false이며 hasUnreadAttachments는 true이다")
        void partiallyDownloaded() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment file1 = mockAttachment(910L, "a.pdf", "file/a.pdf", false);
            OfficialAttachment file2 = mockAttachment(911L, "b.pdf", "file/b.pdf", false);

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(file1, file2));
            given(downloadRepository.findDownloadedAttachmentIds(MEMBER_ID, List.of(910L, 911L)))
                    .willReturn(Set.of(910L));
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.attachments()).hasSize(2);
            assertThat(response.attachments().get(0).id()).isEqualTo(910L);
            assertThat(response.attachments().get(0).isDownloaded()).isTrue();
            assertThat(response.attachments().get(1).id()).isEqualTo(911L);
            assertThat(response.attachments().get(1).isDownloaded()).isFalse();
            assertThat(response.hasUnreadAttachments()).isTrue();
        }

        @Test
        @DisplayName("모든 비이미지 첨부의 다운로드 이력이 존재하면 hasUnreadAttachments는 false이다")
        void allDownloaded() {
            // given
            OfficialPost post = mockPost();
            OfficialPostAiMeta meta = mockMeta(mockTag("장학금"));
            OfficialAttachment file1 = mockAttachment(910L, "a.pdf", "file/a.pdf", false);
            OfficialAttachment file2 = mockAttachment(911L, "b.pdf", "file/b.pdf", false);

            given(postRepository.findActiveByIdAndUniversityScope(POST_ID, UNIVERSITY_ID)).willReturn(Optional.of(post));
            given(aiMetaRepository.findByOfficialPostIdWithTag(POST_ID, ProcessingStatus.DONE))
                    .willReturn(Optional.of(meta));
            given(attachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID))
                    .willReturn(List.of(file1, file2));
            given(downloadRepository.findDownloadedAttachmentIds(MEMBER_ID, List.of(910L, 911L)))
                    .willReturn(Set.of(910L, 911L));
            given(scrapRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);
            given(notificationRepository.existsByMemberIdAndPostId(MEMBER_ID, POST_ID)).willReturn(false);

            // when
            OfficialPostDetailResponse response = officialPostService.getDetail(POST_ID, CONTEXT);

            // then
            assertThat(response.attachments()).allMatch(a -> a.isDownloaded());
            assertThat(response.hasUnreadAttachments()).isFalse();
        }
    }

    @Nested
    @DisplayName("공식 공지 목록 조회")
    class GetList {

        @Nested
        @DisplayName("LATEST 정렬 커서 페이징")
        class LatestPaging {

            @Test
            @DisplayName("size+1 결과가 size를 초과하면 hasNext=true이고 nextCursor는 Base64(publishedAt:id) 형태이다")
            void hasNextTrue() {
                givenScopes();
                stubFindList(IntStream.rangeClosed(1, 21).mapToObj(i -> summaryRaw(i, null)).toList());

                CursorPageResponse<OfficialPostSummaryResponse> result =
                        officialPostService.getList(CONTEXT, null, null, OfficialPostListSort.LATEST, null);

                LocalDate lastPublishedAt = LocalDate.of(2026, 4, 1).minusDays(20);
                String expected = encodeCursor(lastPublishedAt + ":20");
                assertThat(result.hasNext()).isTrue();
                assertThat(result.content()).hasSize(20);
                assertThat(result.nextCursor()).isEqualTo(expected);
            }

            @Test
            @DisplayName("결과가 size 이하이면 hasNext=false이고 nextCursor는 null이다")
            void hasNextFalse() {
                givenScopes();
                stubFindList(List.of(summaryRaw(5L, null), summaryRaw(3L, null)));

                CursorPageResponse<OfficialPostSummaryResponse> result =
                        officialPostService.getList(CONTEXT, null, null, OfficialPostListSort.LATEST, null);

                assertThat(result.hasNext()).isFalse();
                assertThat(result.nextCursor()).isNull();
            }

            @Test
            @DisplayName("Base64(publishedAt:id) cursor를 전달하면 파싱해 리포지토리에 전달한다")
            void withCursor() {
                givenScopes();
                stubFindList(List.of());
                LocalDate cursorDate = LocalDate.of(2026, 4, 1);

                officialPostService.getList(CONTEXT, null, null, OfficialPostListSort.LATEST, encodeCursor(cursorDate + ":100"));

                then(officialPostQueryRepository).should().findList(any(), isNull(), isNull(), eq(OfficialPostListSort.LATEST),
                        eq(cursorDate), eq(100L), isNull(), isNull(), eq(21));
            }

            @Test
            @DisplayName("잘못된 형식의 Base64 cursor이면 INVALID_PARAM 예외가 발생한다")
            void invalidCursor() {
                givenScopes();

                assertThatThrownBy(() -> officialPostService.getList(CONTEXT, null, null, OfficialPostListSort.LATEST, "abc"))
                        .isInstanceOf(BusinessException.class)
                        .extracting(e -> ((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PARAM);
            }
        }

        @Nested
        @DisplayName("DEADLINE 정렬 커서 페이징")
        class DeadlinePaging {

            @Test
            @DisplayName("hasNext=true이면 nextCursor는 Base64(endDate:id) 형태이다")
            void nextCursorEncoding() {
                givenScopes();
                LocalDate endDate = LocalDate.of(2026, 5, 10);
                stubFindList(IntStream.rangeClosed(1, 21).mapToObj(i -> summaryRaw(i, endDate)).toList());

                CursorPageResponse<OfficialPostSummaryResponse> result =
                        officialPostService.getList(CONTEXT, null, null, OfficialPostListSort.DEADLINE, null);

                assertThat(result.nextCursor()).isEqualTo(encodeCursor(endDate + ":20"));
            }

            @Test
            @DisplayName("Base64(date:id) cursor를 전달하면 올바르게 파싱해 리포지토리에 전달한다")
            void withCursor() {
                givenScopes();
                stubFindList(List.of());
                LocalDate cursorDate = LocalDate.of(2026, 5, 10);

                officialPostService.getList(CONTEXT, null, null, OfficialPostListSort.DEADLINE, encodeCursor(cursorDate + ":50"));

                then(officialPostQueryRepository).should().findList(any(), isNull(), isNull(), eq(OfficialPostListSort.DEADLINE),
                        isNull(), isNull(), eq(cursorDate), eq(50L), eq(21));
            }
        }

        @Nested
        @DisplayName("tagCode 필터")
        class TagCodeFilter {

            @Test
            @DisplayName("tagCode를 전달하면 tagId로 변환하여 리포지토리에 전달한다")
            void withTagCode() {
                givenScopes();
                stubFindList(List.of());
                Tag tag = mock(Tag.class);
                given(tag.getId()).willReturn(3L);
                given(tagRepository.findByCode("SCHOLARSHIP")).willReturn(Optional.of(tag));

                officialPostService.getList(CONTEXT, null, "SCHOLARSHIP", OfficialPostListSort.LATEST, null);

                then(officialPostQueryRepository).should().findList(any(), isNull(), eq(3L), eq(OfficialPostListSort.LATEST),
                        isNull(), isNull(), isNull(), isNull(), eq(21));
            }

            @Test
            @DisplayName("존재하지 않는 tagCode이면 INVALID_PARAM 예외가 발생한다")
            void invalidTagCode() {
                givenScopes();
                given(tagRepository.findByCode("UNKNOWN")).willReturn(Optional.empty());

                assertThatThrownBy(() -> officialPostService.getList(CONTEXT, null, "UNKNOWN", OfficialPostListSort.LATEST, null))
                        .isInstanceOf(BusinessException.class)
                        .extracting(e -> ((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PARAM);
            }
        }

        @Test
        @DisplayName("q가 100자를 초과하면 INVALID_PARAM 예외가 발생한다")
        void tooLongKeyword() {
            assertThatThrownBy(() -> officialPostService.getList(CONTEXT, "a".repeat(101), null, OfficialPostListSort.LATEST, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PARAM);
        }

        private void givenScopes() {
            given(memberQueryRepository.findScopesByMemberId(MEMBER_ID)).willReturn(List.of());
        }

        private void stubFindList(List<OfficialPostSummaryRaw> rows) {
            given(officialPostQueryRepository.findList(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .willReturn(rows);
        }

        private String encodeCursor(String raw) {
            return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        private OfficialPostSummaryRaw summaryRaw(long id, LocalDate endDate) {
            return summaryRaw(id, LocalDate.of(2026, 4, 1).minusDays(id), endDate);
        }

        private OfficialPostSummaryRaw summaryRaw(long id, LocalDate publishedAt, LocalDate endDate) {
            return new OfficialPostSummaryRaw(id, "제목" + id, "수강", publishedAt, endDate);
        }
    }

    private OfficialPost mockPost() {
        OfficialPost post = mock(OfficialPost.class);
        lenient().when(post.getId()).thenReturn(POST_ID);
        lenient().when(post.getTitle()).thenReturn("제목");
        lenient().when(post.getPublisher()).thenReturn("학사팀");
        lenient().when(post.getSourceUrl()).thenReturn("https://example.com/notice/1");
        lenient().when(post.getPublishedAt()).thenReturn(LocalDate.of(2026, 4, 1));
        lenient().when(post.getHtmlContent()).thenReturn("<p>본문</p>");
        return post;
    }

    private OfficialPostAiMeta mockMeta(Tag tag) {
        OfficialPostAiMeta meta = mock(OfficialPostAiMeta.class);
        lenient().when(meta.getTag()).thenReturn(tag);
        lenient().when(meta.getSummary()).thenReturn("요약");
        lenient().when(meta.isApplicable()).thenReturn(true);
        lenient().when(meta.getStartDate()).thenReturn(LocalDate.of(2026, 4, 1));
        lenient().when(meta.getStartTime()).thenReturn(LocalTime.of(9, 0));
        lenient().when(meta.getEndDate()).thenReturn(LocalDate.of(2026, 5, 31));
        lenient().when(meta.getEndTime()).thenReturn(null);
        lenient().when(meta.getEligibility()).thenReturn("재학생");
        lenient().when(meta.getApplyMethodType()).thenReturn(ApplyMethodType.FILE);
        lenient().when(meta.getApplyMethodDetail()).thenReturn(null);
        lenient().when(meta.getRequiredDocuments()).thenReturn("성적증명서");
        lenient().when(meta.getContactPhone()).thenReturn("02-1234-5678");
        lenient().when(meta.getContactEmail()).thenReturn("staff@example.com");
        return meta;
    }

    private Tag mockTag(String name) {
        Tag tag = mock(Tag.class);
        lenient().when(tag.getName()).thenReturn(name);
        return tag;
    }

    private OfficialAttachment mockAttachment(Long id, String originalName, String s3Key, boolean isImage) {
        OfficialAttachment attachment = mock(OfficialAttachment.class);
        lenient().when(attachment.getId()).thenReturn(id);
        lenient().when(attachment.getOriginalName()).thenReturn(originalName);
        lenient().when(attachment.getS3Key()).thenReturn(s3Key);
        lenient().when(attachment.isImage()).thenReturn(isImage);
        return attachment;
    }
}