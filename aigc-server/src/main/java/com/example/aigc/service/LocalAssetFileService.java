package com.example.aigc.service;

import com.example.aigc.entity.StoredFileRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.Resource;

@Service
public class LocalAssetFileService {
    private final MediaStorageService mediaStorageService;

    public LocalAssetFileService(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    public StoredFileRecord storeText(String projectId, String relativePath, String mediaType, String content) {
        return mediaStorageService.storeText(projectId, relativePath, mediaType, content);
    }

    public StoredFileRecord storeJson(String projectId, String relativePath, Object value) {
        return mediaStorageService.storeJson(projectId, relativePath, value);
    }

    public StoredFileRecord storeBytes(String projectId, String relativePath, String mediaType, byte[] bytes) {
        return mediaStorageService.storeBytes(projectId, relativePath, mediaType, bytes);
    }

    public StoredFileRecord storeMultipart(String projectId, String relativePath, MultipartFile file, String mediaType) {
        return mediaStorageService.storeMultipart(projectId, relativePath, file, mediaType);
    }

    public StoredFileRecord storeRemote(String projectId, String relativePath, String mediaType, String url) {
        return mediaStorageService.storeRemote(projectId, relativePath, mediaType, url);
    }

    public StoredFileRecord storeBase64(String projectId, String relativePath, String mediaType, String rawBase64) {
        return mediaStorageService.storeBase64(projectId, relativePath, mediaType, rawBase64);
    }

    public String readText(StoredFileRecord record) {
        return mediaStorageService.readText(record);
    }

    public byte[] readBytes(StoredFileRecord record) {
        return mediaStorageService.readBytes(record);
    }

    public Map<String, Object> readJson(StoredFileRecord record) {
        return mediaStorageService.readJson(record);
    }

    public Path resolveStoredFile(StoredFileRecord record) {
        return mediaStorageService.resolveStoredFile(record);
    }

    public Path resolveProjectRoot(String projectId) {
        return mediaStorageService.resolveProjectRoot(projectId);
    }

    public String extractProjectId(String fileId) {
        return mediaStorageService.extractProjectId(fileId);
    }

    public boolean exists(StoredFileRecord record) {
        return mediaStorageService.exists(record);
    }

    public String toPublicUrl(String fileId) {
        return mediaStorageService.toPublicUrl(fileId);
    }

    public Resource openAsResource(StoredFileRecord record) {
        return mediaStorageService.openAsResource(record);
    }
}
