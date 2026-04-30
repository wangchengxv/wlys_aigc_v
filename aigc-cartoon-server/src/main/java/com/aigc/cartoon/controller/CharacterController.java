package com.aigc.cartoon.controller;

import com.aigc.cartoon.common.Result;
import com.aigc.cartoon.entity.Character;
import com.aigc.cartoon.service.CharacterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CharacterController {
    
    private final CharacterService characterService;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;
    
    @Value("${app.upload.max-size-mb:5}")
    private long uploadMaxSizeMb;
    
    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }
    
    @GetMapping("/projects/{projectId}/characters")
    public Result<List<Character>> listByProject(@PathVariable Long projectId) {
        List<Character> characters = characterService.listByProjectId(projectId);
        return Result.success(characters);
    }
    
    @GetMapping("/characters/{id}")
    public Result<Character> getById(@PathVariable Long id) {
        Character character = characterService.getById(id);
        if (character == null) {
            return Result.error("角色不存在");
        }
        return Result.success(character);
    }
    
    @PostMapping("/characters")
    public Result<Character> create(@RequestBody Character character) {
        characterService.save(character);
        return Result.success(character);
    }
    
    @PutMapping("/characters/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Character character) {
        character.setId(id);
        boolean success = characterService.updateById(character);
        return Result.success(success);
    }
    
    @DeleteMapping("/characters/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = characterService.removeById(id);
        return Result.success(success);
    }
    
    @PostMapping("/characters/upload-image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.badRequest("请先选择图片");
        }
        if (file.getSize() > uploadMaxSizeMb * 1024 * 1024) {
            return Result.badRequest("图片大小不能超过" + uploadMaxSizeMb + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return Result.badRequest("仅支持 JPG、PNG、WEBP 格式图片");
        }
        
        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        if (extension == null) {
            return Result.badRequest("无法识别文件扩展名");
        }
        
        String fileName = FILE_NAME_FORMATTER.format(LocalDateTime.now())
            + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
            + "."
            + extension;
        
        Path uploadDir = Path.of(uploadBaseDir, "character-images").toAbsolutePath().normalize();
        Path targetFile = uploadDir.resolve(fileName);
        
        try {
            Files.createDirectories(uploadDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            return Result.error("图片上传失败，请稍后重试");
        }
        
        String url = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/uploads/character-images/")
            .path(fileName)
            .toUriString();
        return Result.success(Map.of("url", url));
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "jpg";
        }
        if ("png".equals(extension)) {
            return "png";
        }
        if ("webp".equals(extension)) {
            return "webp";
        }
        return null;
    }
}