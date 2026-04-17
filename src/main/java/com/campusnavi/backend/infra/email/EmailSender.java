package com.campusnavi.backend.infra.email;

public interface EmailSender {
    void send(String to, String subject, String content);
}
