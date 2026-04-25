package com.campusnavi.backend.official.crawler.parser;

import com.campusnavi.backend.official.crawler.dto.PostList;
import com.campusnavi.backend.official.crawler.dto.PostDetail;

import java.util.List;

public interface CrawlParser {
    List<PostList> fetchList(String listUrl, int page);
    PostDetail fetchDetail(String detailUrl);
}
