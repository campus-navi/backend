package com.campusnavi.backend.global.response;

import java.util.List;

public record CursorPageResponse <T> (
        List<T> content,
        String nextCursor,
        boolean hasNext
) {
    public static <T> CursorPageResponse <T> of(List<T> content, String nextCursor, boolean hasNext) {
        return new CursorPageResponse<>(content,nextCursor,hasNext);
    }
}
