package com.campusnavi.backend.auth.dto;

public record EmailSendRequest(
        Long campusId,
        String email
) {
}
