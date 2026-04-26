package com.campusnavi.backend.official.crawler.parser;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CrawlParserFactory {

    private final Map<String, CrawlParser> parserMap;

    public CrawlParserFactory(Map<String, CrawlParser> parserMap) {
        this.parserMap = parserMap;
    }

    public CrawlParser getParser(String parserType) {
        CrawlParser parser = parserMap.get(parserType);
        if (parser == null) {
            throw new BusinessException(ErrorCode.INVALID_PARSER_TYPE);
        }
        return parser;
    }
}
