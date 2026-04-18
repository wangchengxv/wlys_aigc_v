package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.service.MediaResourceService;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/media-resources")
public class MediaResourceController {
    private final MediaResourceService mediaResourceService;
    private final RequestAuthService requestAuthService;

    public MediaResourceController(
            MediaResourceService mediaResourceService,
            RequestAuthService requestAuthService
    ) {
        this.mediaResourceService = mediaResourceService;
        this.requestAuthService = requestAuthService;
    }

    @GetMapping
    public ApiResponse<List<StoredFileRecord>> listRecent(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId
    ) {
        RequestUserContext actor = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(mediaResourceService.listRecent(actor));
    }
}
