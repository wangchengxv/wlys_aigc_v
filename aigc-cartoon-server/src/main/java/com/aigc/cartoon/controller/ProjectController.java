package com.aigc.cartoon.controller;

import com.aigc.cartoon.common.Result;
import com.aigc.cartoon.entity.Project;
import com.aigc.cartoon.entity.ProjectConfig;
import com.aigc.cartoon.service.ProjectService;
import com.aigc.cartoon.service.ProjectConfigService;
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
@RequestMapping("/api/projects")
public class ProjectController {
    
    private final ProjectService projectService;
    private final ProjectConfigService projectConfigService;
    private static final String DEFAULT_ASPECT_RATIO = "16:9";
    private static final String DEFAULT_VISUAL_STYLE_MODE = "preset";
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
    
    public ProjectController(ProjectService projectService, ProjectConfigService projectConfigService) {
        this.projectService = projectService;
        this.projectConfigService = projectConfigService;
    }
    
    @GetMapping
    public Result<List<Project>> list() {
        List<Project> projects = projectService.list();
        projects.forEach(this::applyProjectFallbacks);
        return Result.success(projects);
    }
    
    @GetMapping("/{id}")
    public Result<Project> getById(@PathVariable Long id) {
        Project project = projectService.getById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }
        applyProjectFallbacks(project);
        return Result.success(project);
    }
    
    @PostMapping
    public Result<Project> create(@RequestBody Project project) {
        applyProjectFallbacks(project);
        projectService.save(project);
        ProjectConfig config = new ProjectConfig();
        config.setProjectId(project.getId());
        projectConfigService.save(config);
        applyProjectFallbacks(project);
        return Result.success(project);
    }
    
    @PostMapping("/upload-cover")
    public Result<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.badRequest("请先选择封面图片");
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
        
        Path uploadDir = Path.of(uploadBaseDir, "project-covers").toAbsolutePath().normalize();
        Path targetFile = uploadDir.resolve(fileName);
        
        try {
            Files.createDirectories(uploadDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            return Result.error("封面上传失败，请稍后重试");
        }
        
        String url = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/uploads/project-covers/")
            .path(fileName)
            .toUriString();
        return Result.success(Map.of("url", url));
    }
    
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        boolean success = projectService.updateById(project);
        return Result.success(success);
    }
    
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = projectService.removeById(id);
        return Result.success(success);
    }
    
    @PutMapping("/{id}/rename")
    public Result<Boolean> rename(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String newName = body.get("name");
        boolean success = projectService.rename(id, newName);
        return Result.success(success);
    }
    
    @GetMapping("/{id}/config")
    public Result<ProjectConfig> getConfig(@PathVariable Long id) {
        ProjectConfig config = projectConfigService.getByProjectId(id);
        if (config == null) {
            config = new ProjectConfig();
            config.setProjectId(id);
            projectConfigService.save(config);
        }
        return Result.success(config);
    }
    
    @PutMapping("/{id}/config")
    public Result<Boolean> updateConfig(@PathVariable Long id, @RequestBody ProjectConfig config) {
        config.setProjectId(id);
        ProjectConfig existing = projectConfigService.getByProjectId(id);
        if (existing != null) {
            config.setId(existing.getId());
            return Result.success(projectConfigService.update(config));
        } else {
            return Result.success(projectConfigService.save(config));
        }
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
    
    private void applyProjectFallbacks(Project project) {
        if (project == null) {
            return;
        }
        if (project.getUserId() == null) {
            project.setUserId(1L);
        }
        if (isBlank(project.getAspectRatio())) {
            project.setAspectRatio(DEFAULT_ASPECT_RATIO);
        }
        
        String legacyStyle = trimToNull(project.getStyle());
        String styleTemplateId = trimToNull(project.getStyleTemplateId());
        if (legacyStyle == null && styleTemplateId != null) {
            legacyStyle = styleTemplateId;
        }
        if (styleTemplateId == null && legacyStyle != null) {
            styleTemplateId = legacyStyle;
        }
        project.setStyle(legacyStyle == null ? "" : legacyStyle);
        project.setStyleTemplateId(styleTemplateId == null ? project.getStyle() : styleTemplateId);
        
        String customStyleText = trimToNull(project.getCustomStyleText());
        String visualStylePrompt = trimToNull(project.getVisualStylePrompt());
        if (visualStylePrompt == null && customStyleText != null) {
            visualStylePrompt = customStyleText;
        }
        if (project.getVisualStyleLongTextMode() == null) {
            project.setVisualStyleLongTextMode(customStyleText != null);
        }
        if (customStyleText == null && Boolean.TRUE.equals(project.getVisualStyleLongTextMode()) && visualStylePrompt != null) {
            customStyleText = visualStylePrompt;
        }
        project.setVisualStylePrompt(visualStylePrompt == null ? "" : visualStylePrompt);
        project.setCustomStyleText(customStyleText == null ? "" : customStyleText);
        
        String visualStyleMode = trimToNull(project.getVisualStyleMode());
        if (visualStyleMode == null) {
            visualStyleMode = Boolean.TRUE.equals(project.getVisualStyleLongTextMode()) ? "custom" : DEFAULT_VISUAL_STYLE_MODE;
        }
        project.setVisualStyleMode(visualStyleMode);
    }
    
    private boolean isBlank(String value) {
        return trimToNull(value) == null;
    }
    
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
