package com.example.aigc.controller;

import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.exception.BizException;
import com.example.aigc.service.LocalAssetFileService;
import com.example.aigc.service.ScriptProjectService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/files")
public class FileAssetController {

    private final ScriptProjectService scriptProjectService;
    private final LocalAssetFileService localAssetFileService;

    public FileAssetController(
            ScriptProjectService scriptProjectService,
            LocalAssetFileService localAssetFileService
    ) {
        this.scriptProjectService = scriptProjectService;
        this.localAssetFileService = localAssetFileService;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> preview(@PathVariable String fileId) {
        return buildFileResponse(fileId, false);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        return buildFileResponse(fileId, true);
    }

    private ResponseEntity<Resource> buildFileResponse(String fileId, boolean attachment) {
        String projectId = localAssetFileService.extractProjectId(fileId);
        if (projectId == null) {
            throw new BizException(404, "文件不存在");
        }
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        StoredFileRecord record = scriptProjectService.findFile(aggregate, fileId);
        if (record == null) {
            throw new BizException(404, "文件不存在");
        }
        Resource resource = localAssetFileService.openAsResource(record);
        if (!resource.exists()) {
            throw new BizException(404, "文件不存在");
        }
        HttpHeaders headers = new HttpHeaders();
        if (attachment) {
            headers.setContentDisposition(ContentDisposition.attachment().filename(record.fileName).build());
        } else {
            headers.setContentDisposition(ContentDisposition.inline().filename(record.fileName).build());
        }
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(record.mediaType == null ? "application/octet-stream" : record.mediaType))
                .body(resource);
    }
}
