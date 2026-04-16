package com.campusnavi.backend.infra.storage;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public void upload(UploadType type, String filename, InputStream inputStream, long size, String contentType) {
        validate(type, contentType, size);
        String key = generateKey(type, filename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();

        try (InputStream stream = inputStream) {
            s3Client.putObject(request, RequestBody.fromInputStream(stream, size));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
        }
    }

    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }

    public boolean exists(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();

        try {
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    public PresignedUrlResponse generatePresignedUrl(UploadType type, String filename, String contentType, long size) {
        validate(type, contentType, size);
        String key = generateKey(type, filename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .putObjectRequest(request)
                .build();

        String url = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        return new PresignedUrlResponse(url, key);
    }

    public String resolveUrl(String key) {
        return properties.baseUrl() + "/" + key;
    }

    private void validate(UploadType type, String contentType, long size) {
        if (!type.isContentTypeAllowed(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_CONTENT_TYPE);
        }
        if (size <= 0) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
        }
        if (size > type.getMaxBytes()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    private String generateKey(UploadType type, String filename) {
        return type.getPrefix() + UUID.randomUUID() + extractExtension(filename);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1)
                .replaceAll("[^a-zA-Z0-9]", "");
        return extension.isEmpty() ? "" : "." + extension;
    }
}
