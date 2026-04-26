package com.campusnavi.backend.official.crawler.service;

import com.campusnavi.backend.infra.storage.S3StorageService;
import com.campusnavi.backend.infra.storage.UploadType;
import com.campusnavi.backend.official.crawler.dto.FileInfo;
import com.campusnavi.backend.official.crawler.dto.PostDetail;
import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.dto.UploadedFile;
import com.campusnavi.backend.official.crawler.parser.CrawlParser;
import com.campusnavi.backend.official.entity.OfficialSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlPostService {

    private final S3StorageService s3StorageService;
    private final CrawlerSaveService crawlerSaveService;

    public void crawlAndSave(OfficialSource source, PostList list, CrawlParser parser) throws Exception {
        PostDetail post = parser.fetchDetail(list.detailUrl());

        List<UploadedFile> uploadedImages = uploadImages(post.images());
        List<UploadedFile> uploadedAttachments = uploadAttachments(post.attachments());

        Map<String, String> imageUrlMap = buildImageUrlMap(uploadedImages);
        String replacedHtml = replaceImageUrls(post.rawHtml(), imageUrlMap);
        String sanitizedHtml = Jsoup.clean(replacedHtml, Safelist.relaxed());

        crawlerSaveService.savePost(source, list, post, sanitizedHtml, uploadedImages, uploadedAttachments);
    }

    private List<UploadedFile> uploadImages(List<FileInfo> images) throws Exception {
        List<UploadedFile> result = new ArrayList<>();
        for (FileInfo img : images) {
            String s3Key = uploadFromUrl(img.originalUrl(), img.originalName(), img.contentType(), UploadType.INFO_IMAGE);
            result.add(new UploadedFile(img.originalUrl(), img.originalName(), s3Key, img.contentType()));
        }
        return result;
    }

    private List<UploadedFile> uploadAttachments(List<FileInfo> attachments) throws Exception {
        List<UploadedFile> result = new ArrayList<>();
        for (FileInfo attach : attachments) {
            String s3Key = uploadFromUrl(attach.originalUrl(), attach.originalName(), attach.contentType(), UploadType.INFO_ATTACH);
            result.add(new UploadedFile(attach.originalUrl(), attach.originalName(), s3Key, attach.contentType()));
        }
        return result;
    }

    private String uploadFromUrl(String url, String filename, String contentType, UploadType type) throws Exception {
        byte[] bytes = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(15_000)
                .execute()
                .bodyAsBytes();
        return s3StorageService.upload(type, filename, new ByteArrayInputStream(bytes), bytes.length, contentType);
    }

    private Map<String, String> buildImageUrlMap(List<UploadedFile> uploadedImages) {
        Map<String, String> urlMap = new LinkedHashMap<>();
        for (UploadedFile ui : uploadedImages) {
            urlMap.put(ui.originalUrl(), s3StorageService.resolveUrl(ui.s3Key()));
        }
        return urlMap;
    }

    private String replaceImageUrls(String rawHtml, Map<String, String> imageUrlMap) {
        if (rawHtml == null || imageUrlMap.isEmpty()) return rawHtml;
        Document doc = Jsoup.parse(rawHtml);
        for (Element img : doc.select("img[src]")) {
            String src = img.attr("src");
            String s3Url = imageUrlMap.get(src);
            if (s3Url != null) {
                img.attr("src", s3Url);
            }
        }
        return doc.body().html();
    }
}