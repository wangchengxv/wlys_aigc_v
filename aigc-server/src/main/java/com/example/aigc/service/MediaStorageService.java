package com.example.aigc.service;

import com.example.aigc.entity.StoredFileRecord;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

public interface MediaStorageService {
    StoredFileRecord storeText(String projectId, String relativePath, String mediaType, String content);

    StoredFileRecord storeJson(String projectId, String relativePath, Object value);

    StoredFileRecord storeBytes(String projectId, String relativePath, String mediaType, byte[] bytes);

    StoredFileRecord storeMultipart(String projectId, String relativePath, MultipartFile file, String mediaType);

    StoredFileRecord storeRemote(String projectId, String relativePath, String mediaType, String url);

    StoredFileRecord storeBase64(String projectId, String relativePath, String mediaType, String rawBase64);

    String readText(StoredFileRecord record);

    byte[] readBytes(StoredFileRecord record);

    Map<String, Object> readJson(StoredFileRecord record);

    Path resolveStoredFile(StoredFileRecord record);

    Path resolveProjectRoot(String projectId);

    boolean exists(StoredFileRecord record);

    String toPublicUrl(String fileId);

    String extractProjectId(String fileId);

    Resource openAsResource(StoredFileRecord record);
}
