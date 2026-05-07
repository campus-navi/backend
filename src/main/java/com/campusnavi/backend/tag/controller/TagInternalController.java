package com.campusnavi.backend.tag.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.tag.dto.InternalTagResponse;
import com.campusnavi.backend.tag.service.TagService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/tags")
public class TagInternalController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InternalTagResponse>>> getTagsForInternal() {
        return ResponseEntity.ok(ApiResponse.ok(tagService.getAllTagsForInternal()));
    }
}
