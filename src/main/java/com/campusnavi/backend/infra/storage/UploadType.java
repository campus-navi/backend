package com.campusnavi.backend.infra.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadType {

    POST_IMAGE("posts/images/", "image/*", 10L * 1024 * 1024),   // 10MB
    INFO_IMAGE("information/images/", "image/*", 10L * 1024 * 1024),   // 10MB
    INFO_ATTACH("information/attachments/", "*/*", 50L * 1024 * 1024);   // 50MB

    private final String prefix;
    private final String contentType;
    private final long maxBytes;

    public boolean isContentTypeAllowed(String type) {
        if (type == null || !type.contains("/")) return false;
        if (this.contentType.equals("*/*")) return true;
        String allowedPrefix = this.contentType.replace("/*", "/");
        return type.startsWith(allowedPrefix);
    }
}
