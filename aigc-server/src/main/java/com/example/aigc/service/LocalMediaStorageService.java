package com.example.aigc.service;

import com.example.aigc.config.StorageProperties;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.enums.StorageProvider;
import com.example.aigc.repository.JsonFileStorageSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalMediaStorageService implements MediaStorageService {
    private final JsonFileStorageSupport storageSupport;
    private final HttpClient httpClient;
    private final StorageProperties storageProperties;

    public LocalMediaStorageService(
            JsonFileStorageSupport storageSupport,
            StorageProperties storageProperties
    ) {
        this.storageSupport = storageSupport;
        this.storageProperties = storageProperties;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public StoredFileRecord storeText(String projectId, String relativePath, String mediaType, String content) {
        Path target = resolveProjectRoot(projectId).resolve(normalizeRelativePath(relativePath));
        storageSupport.writeString(target, content == null ? "" : content);
        return buildRecord(projectId, target, mediaType == null ? "text/plain; charset=UTF-8" : mediaType);
    }

    @Override
    public StoredFileRecord storeJson(String projectId, String relativePath, Object value) {
        Path target = resolveProjectRoot(projectId).resolve(normalizeRelativePath(relativePath));
        storageSupport.writeValue(target, value);
        return buildRecord(projectId, target, "application/json");
    }

    @Override
    public StoredFileRecord storeBytes(String projectId, String relativePath, String mediaType, byte[] bytes) {
        Path target = resolveProjectRoot(projectId).resolve(normalizeRelativePath(relativePath));
        storageSupport.writeBytes(target, bytes == null ? new byte[0] : bytes);
        return buildRecord(projectId, target, mediaType);
    }

    @Override
    public StoredFileRecord storeMultipart(String projectId, String relativePath, MultipartFile file, String mediaType) {
        try {
            return storeBytes(projectId, relativePath, mediaType == null ? defaultMediaType(file.getOriginalFilename()) : mediaType, file.getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("保存上传文件失败", ex);
        }
    }

    @Override
    public StoredFileRecord storeRemote(String projectId, String relativePath, String mediaType, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("下载远程文件失败: " + response.statusCode());
            }
            return storeBytes(projectId, relativePath, mediaType, response.body());
        } catch (Exception ex) {
            throw new IllegalStateException("下载远程文件失败", ex);
        }
    }

    @Override
    public StoredFileRecord storeBase64(String projectId, String relativePath, String mediaType, String rawBase64) {
        String normalized = rawBase64 == null ? "" : rawBase64.trim();
        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            normalized = normalized.substring(commaIndex + 1);
        }
        byte[] bytes = Base64.getDecoder().decode(normalized.getBytes(StandardCharsets.UTF_8));
        return storeBytes(projectId, relativePath, mediaType, bytes);
    }

    @Override
    public String readText(StoredFileRecord record) {
        return storageSupport.readString(resolveStoredFile(record), () -> "");
    }

    @Override
    public byte[] readBytes(StoredFileRecord record) {
        try {
            return Files.readAllBytes(resolveStoredFile(record));
        } catch (IOException ex) {
            throw new IllegalStateException("读取文件失败", ex);
        }
    }

    @Override
    public Map<String, Object> readJson(StoredFileRecord record) {
        return storageSupport.readValue(
                resolveStoredFile(record),
                new TypeReference<>() {
                },
                LinkedHashMap::new
        );
    }

    @Override
    public Path resolveStoredFile(StoredFileRecord record) {
        String objectKey = record.objectKey == null || record.objectKey.isBlank() ? record.relativePath : record.objectKey;
        return resolveProjectRoot(record.projectId).resolve(normalizeRelativePath(objectKey)).normalize();
    }

    @Override
    public Path resolveProjectRoot(String projectId) {
        return storageSupport.resolve("script-projects/" + projectId);
    }

    @Override
    public boolean exists(StoredFileRecord record) {
        return Files.exists(resolveStoredFile(record));
    }

    @Override
    public String toPublicUrl(String fileId) {
        return "/api/v1/files/" + fileId;
    }

    @Override
    public String extractProjectId(String fileId) {
        if (fileId == null || !fileId.contains("__")) {
            return null;
        }
        return fileId.substring(0, fileId.indexOf("__"));
    }

    @Override
    public Resource openAsResource(StoredFileRecord record) {
        return new FileSystemResource(resolveStoredFile(record));
    }

    private StoredFileRecord buildRecord(String projectId, Path target, String mediaType) {
        StoredFileRecord record = new StoredFileRecord();
        record.projectId = projectId;
        record.fileId = projectId + "__" + UUID.randomUUID().toString().replace("-", "");
        record.fileName = target.getFileName().toString();
        record.relativePath = resolveProjectRoot(projectId).relativize(target).toString().replace('\\', '/');
        record.storageProvider = StorageProvider.LOCAL;
        record.bucketName = storageProperties.getLocalBucket();
        record.objectKey = record.relativePath;
        record.publicUrl = toPublicUrl(record.fileId);
        record.mediaType = mediaType == null || mediaType.isBlank() ? defaultMediaType(record.fileName) : mediaType;
        try {
            record.sizeBytes = Files.size(target);
        } catch (IOException ex) {
            record.sizeBytes = 0L;
        }
        record.createdAt = Instant.now();
        return record;
    }

    private String normalizeRelativePath(String relativePath) {
        String normalized = relativePath == null ? "" : relativePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        normalized = normalized.replace("..", "");
        return normalized;
    }

    private String defaultMediaType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".md") || lower.endsWith(".txt")) {
            return "text/plain; charset=UTF-8";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        return "application/octet-stream";
    }
}
