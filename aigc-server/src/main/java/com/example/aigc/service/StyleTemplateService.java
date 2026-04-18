package com.example.aigc.service;

import com.example.aigc.dto.StyleTemplateCreateRequest;
import com.example.aigc.dto.StyleTemplateUpdateRequest;
import com.example.aigc.entity.StyleTemplate;
import com.example.aigc.enums.StyleTemplateScope;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.StyleTemplateRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class StyleTemplateService {
    private final StyleTemplateRepository styleTemplateRepository;
    private final AuditLogService auditLogService;

    public StyleTemplateService(
            StyleTemplateRepository styleTemplateRepository,
            AuditLogService auditLogService
    ) {
        this.styleTemplateRepository = styleTemplateRepository;
        this.auditLogService = auditLogService;
    }

    public List<StyleTemplate> listVisible(RequestUserContext userContext) {
        return styleTemplateRepository.findAll().stream()
                .filter(item -> item.enabled)
                .filter(item -> isVisible(item, userContext))
                .sorted(Comparator
                        .comparing((StyleTemplate item) -> scopeOrder(item.scope))
                        .thenComparing(item -> blankToTilde(item.category))
                        .thenComparing(item -> blankToTilde(item.name)))
                .toList();
    }

    public StyleTemplate requireVisible(String templateId, RequestUserContext userContext) {
        StyleTemplate template = styleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BizException(404, "风格模板不存在"));
        if (!template.enabled || !isVisible(template, userContext)) {
            throw new BizException(404, "风格模板不存在");
        }
        return template;
    }

    public StyleTemplate requireVisibleForCourse(String templateId, RequestUserContext userContext, String courseId) {
        String effectiveCourseId = blankToNull(courseId);
        RequestUserContext effectiveContext = effectiveCourseId == null
                ? userContext
                : new RequestUserContext(
                        userContext.userId(),
                        userContext.userName(),
                        userContext.role(),
                        userContext.orgUnitId(),
                        effectiveCourseId,
                        userContext.authenticated()
                );
        StyleTemplate template = requireVisible(templateId, effectiveContext);
        if (template.scope == StyleTemplateScope.COURSE && !Objects.equals(blankToNull(template.courseId), effectiveCourseId)) {
            throw new BizException(400, "风格模板不属于当前课程");
        }
        return template;
    }

    public StyleTemplate create(RequestUserContext userContext, StyleTemplateCreateRequest request) {
        StyleTemplateScope scope = request.scope() == null ? StyleTemplateScope.PERSONAL : request.scope();
        if (scope == StyleTemplateScope.SYSTEM) {
            throw new BizException(403, "系统模板只允许通过系统初始化");
        }
        String effectiveCourseId = scope == StyleTemplateScope.COURSE
                ? firstNonBlank(request.courseId(), userContext.courseId())
                : null;
        if (scope == StyleTemplateScope.COURSE && (effectiveCourseId == null || effectiveCourseId.isBlank())) {
            throw new BizException(400, "课程模板必须绑定课程ID");
        }

        StyleTemplate template = new StyleTemplate();
        template.templateId = nextId();
        template.scope = scope;
        template.name = request.name().trim();
        template.category = blankToNull(request.category());
        template.traits = blankToNull(request.traits());
        template.fullPrompt = request.fullPrompt().trim();
        template.styleKey = blankToNull(request.styleKey());
        template.ownerId = userContext.userId();
        template.ownerName = firstNonBlank(userContext.userName(), userContext.userId());
        template.orgUnitId = userContext.orgUnitId();
        template.courseId = blankToNull(effectiveCourseId);
        template.enabled = true;
        template.createdAt = Instant.now();
        template.updatedAt = template.createdAt;

        StyleTemplate saved = styleTemplateRepository.save(template);
        auditLogService.record(userContext, "STYLE_TEMPLATE_CREATED", "STYLE_TEMPLATE", saved.templateId, Map.of(
                "scope", saved.scope.name(),
                "name", saved.name
        ));
        return saved;
    }

    public StyleTemplate update(RequestUserContext userContext, String templateId, StyleTemplateUpdateRequest request) {
        StyleTemplate template = styleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BizException(404, "风格模板不存在"));
        if (template.scope == StyleTemplateScope.SYSTEM) {
            throw new BizException(403, "系统模板暂不支持在线编辑");
        }
        if (!Objects.equals(template.ownerId, userContext.userId())) {
            throw new BizException(403, "只能编辑自己的风格模板");
        }

        if (request.name() != null && !request.name().isBlank()) {
            template.name = request.name().trim();
        }
        if (request.category() != null) {
            template.category = blankToNull(request.category());
        }
        if (request.traits() != null) {
            template.traits = blankToNull(request.traits());
        }
        if (request.fullPrompt() != null && !request.fullPrompt().isBlank()) {
            template.fullPrompt = request.fullPrompt().trim();
        }
        if (request.styleKey() != null) {
            template.styleKey = blankToNull(request.styleKey());
        }
        if (request.enabled() != null) {
            template.enabled = request.enabled();
        }
        template.updatedAt = Instant.now();

        StyleTemplate saved = styleTemplateRepository.save(template);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", saved.name);
        details.put("enabled", saved.enabled);
        details.put("scope", saved.scope.name());
        auditLogService.record(userContext, "STYLE_TEMPLATE_UPDATED", "STYLE_TEMPLATE", saved.templateId, details);
        return saved;
    }

    private boolean isVisible(StyleTemplate template, RequestUserContext userContext) {
        if (template.scope == null || template.scope == StyleTemplateScope.SYSTEM) {
            return true;
        }
        if (template.scope == StyleTemplateScope.PERSONAL) {
            return Objects.equals(template.ownerId, userContext.userId());
        }
        String courseId = blankToNull(userContext.courseId());
        return courseId != null && Objects.equals(courseId, template.courseId);
    }

    private int scopeOrder(StyleTemplateScope scope) {
        if (scope == null) return 0;
        return switch (scope) {
            case SYSTEM -> 0;
            case COURSE -> 1;
            case PERSONAL -> 2;
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToTilde(String value) {
        return value == null || value.isBlank() ? "~" : value.trim();
    }

    private String firstNonBlank(String a, String b) {
        String first = blankToNull(a);
        if (first != null) {
            return first;
        }
        return blankToNull(b);
    }

    private String nextId() {
        return "style-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
