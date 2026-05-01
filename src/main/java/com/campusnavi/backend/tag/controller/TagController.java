package com.campusnavi.backend.tag.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.tag.dto.TagResponse;
import com.campusnavi.backend.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "5. 관심사", description = "관심사 태그 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "관심사 목록 조회", description = "온보딩·마이페이지에서 선택 가능한 관심사 목록을 반환한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags() {
        return ResponseEntity.ok(ApiResponse.ok(tagService.getTags()));
    }
}
