package com.campusnavi.backend.official.crawler.service;

import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.dto.PostDetail;
import com.campusnavi.backend.official.crawler.dto.UploadedFile;
import com.campusnavi.backend.official.entity.OfficialAttachment;
import com.campusnavi.backend.official.entity.OfficialPost;
import com.campusnavi.backend.official.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.entity.OfficialSource;
import com.campusnavi.backend.official.ai.event.OfficialPostSavedEvent;
import com.campusnavi.backend.official.repository.OfficialAttachmentRepository;
import com.campusnavi.backend.official.repository.OfficialPostAiMetaRepository;
import com.campusnavi.backend.official.repository.OfficialPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrawlerSaveService {

    private final OfficialPostRepository postRepository;
    private final OfficialAttachmentRepository attachmentRepository;
    private final OfficialPostAiMetaRepository aiMetaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void savePost(OfficialSource source, PostList item, PostDetail detail,
                         String replacedHtml,
                         List<UploadedFile> images,
                         List<UploadedFile> attachments) {
        OfficialPost post = OfficialPost.create(
                source,
                item.originalId(),
                detail.title(),
                item.publisher(),
                detail.structuredText(),
                replacedHtml,
                item.detailUrl(),
                item.publishedAt(),
                LocalDateTime.now()
        );

        postRepository.save(post);

        List<OfficialAttachment> allAttachments = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            UploadedFile f = images.get(i);
            allAttachments.add(OfficialAttachment.create(post, f.originalName(), f.s3Key(), f.contentType(), true, (short) i));
        }
        for (int i = 0; i < attachments.size(); i++) {
            UploadedFile f = attachments.get(i);
            allAttachments.add(OfficialAttachment.create(post, f.originalName(), f.s3Key(), f.contentType(), false, (short) i));
        }
        if (!allAttachments.isEmpty()) {
            attachmentRepository.saveAll(allAttachments);
        }

        aiMetaRepository.save(OfficialPostAiMeta.pending(post));
        eventPublisher.publishEvent(new OfficialPostSavedEvent(post.getId()));
    }

}