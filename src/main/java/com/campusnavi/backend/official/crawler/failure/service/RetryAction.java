package com.campusnavi.backend.official.crawler.failure.service;

@FunctionalInterface
public interface RetryAction {

    void execute() throws Exception;
}
