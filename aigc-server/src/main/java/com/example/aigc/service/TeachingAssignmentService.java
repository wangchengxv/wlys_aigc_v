package com.example.aigc.service;

import com.example.aigc.dto.AssignmentCreateRequest;
import com.example.aigc.entity.TeachingAssignment;
import com.example.aigc.entity.TeachingCourse;
import com.example.aigc.enums.AssignmentStatus;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.TeachingAssignmentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TeachingAssignmentService {
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TeachingCourseService teachingCourseService;
    private final StyleTemplateService styleTemplateService;
    private final AuditLogService auditLogService;
    private final AuthorizationService authorizationService;

    public TeachingAssignmentService(
            TeachingAssignmentRepository teachingAssignmentRepository,
            TeachingCourseService teachingCourseService,
            StyleTemplateService styleTemplateService,
            AuditLogService auditLogService,
            AuthorizationService authorizationService
    ) {
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.teachingCourseService = teachingCourseService;
        this.styleTemplateService = styleTemplateService;
        this.auditLogService = auditLogService;
        this.authorizationService = authorizationService;
    }

    public List<TeachingAssignment> listByCourse(String courseId, RequestUserContext userContext) {
        teachingCourseService.requireVisible(courseId, userContext);
        return teachingAssignmentRepository.findAllByCourseId(courseId).stream()
                .sorted(Comparator
                        .comparing((TeachingAssignment item) -> item.dueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(item -> item.createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public TeachingAssignment create(String courseId, RequestUserContext userContext, AssignmentCreateRequest request) {
        TeachingCourse course = teachingCourseService.requireManageable(courseId, userContext);
        if (course.archived) {
            throw new BizException(400, "课程已归档，不能继续发布新作业");
        }
        Instant dueAt = request.dueAt().toInstant();
        if (!dueAt.isAfter(Instant.now())) {
            throw new BizException(400, "最晚提交时间必须晚于当前时间");
        }
        String styleTemplateId = blankToNull(request.styleTemplateId());
        if (styleTemplateId != null) {
            styleTemplateService.requireVisibleForCourse(styleTemplateId, userContext, course.courseId);
        }

        TeachingAssignment assignment = new TeachingAssignment();
        assignment.assignmentId = nextId();
        assignment.courseId = course.courseId;
        assignment.title = request.title().trim();
        assignment.brief = blankToNull(request.brief());
        assignment.styleTemplateId = styleTemplateId;
        assignment.aspectRatio = blankToNull(request.aspectRatio());
        assignment.targetDuration = request.targetDuration();
        assignment.language = blankToNull(request.language());
        // API 入参使用带时区时间，持久化时统一转换为 UTC Instant。
        assignment.dueAt = dueAt;
        assignment.ownerId = userContext.userId();
        assignment.ownerName = firstNonBlank(userContext.userName(), userContext.userId());
        assignment.status = AssignmentStatus.PUBLISHED;
        assignment.createdAt = Instant.now();
        assignment.updatedAt = assignment.createdAt;

        TeachingAssignment saved = teachingAssignmentRepository.save(assignment);
        auditLogService.record(userContext, "ASSIGNMENT_CREATED", "ASSIGNMENT", saved.assignmentId, Map.of(
                "courseId", saved.courseId,
                "title", saved.title
        ));
        return saved;
    }

    public TeachingAssignment requireVisible(String assignmentId, RequestUserContext userContext) {
        TeachingAssignment assignment = teachingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BizException(404, "作业不存在"));
        teachingCourseService.requireVisible(assignment.courseId, userContext);
        return assignment;
    }

    public TeachingAssignment requireReviewable(String assignmentId, RequestUserContext userContext) {
        TeachingAssignment assignment = requireVisible(assignmentId, userContext);
        if (!authorizationService.canReviewAssignment(assignment, userContext)) {
            throw new BizException(403, "只有作业发布者或管理员可以评分");
        }
        return assignment;
    }

    public TeachingAssignment updateStatus(String assignmentId, RequestUserContext userContext, AssignmentStatus status) {
        TeachingAssignment assignment = requireReviewable(assignmentId, userContext);
        if (assignment.status == status) {
            return assignment;
        }
        assignment.status = status;
        assignment.updatedAt = Instant.now();
        TeachingAssignment saved = teachingAssignmentRepository.save(assignment);
        auditLogService.record(userContext, "ASSIGNMENT_STATUS_UPDATED", "ASSIGNMENT", saved.assignmentId, Map.of(
                "courseId", saved.courseId,
                "status", saved.status.name(),
                "title", saved.title
        ));
        return saved;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        String a = blankToNull(first);
        if (a != null) {
            return a;
        }
        return blankToNull(second);
    }

    private String nextId() {
        return "assignment-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
