package com.example.aigc.service;

import com.example.aigc.dto.CourseCreateRequest;
import com.example.aigc.entity.TeachingCourse;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.TeachingCourseRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class TeachingCourseService {
    private final TeachingCourseRepository teachingCourseRepository;
    private final AuditLogService auditLogService;
    private final AuthorizationService authorizationService;

    public TeachingCourseService(
            TeachingCourseRepository teachingCourseRepository,
            AuditLogService auditLogService,
            AuthorizationService authorizationService
    ) {
        this.teachingCourseRepository = teachingCourseRepository;
        this.auditLogService = auditLogService;
        this.authorizationService = authorizationService;
    }

    public List<TeachingCourse> listVisible(RequestUserContext userContext) {
        return teachingCourseRepository.findAll().stream()
                .filter(course -> authorizationService.canViewCourse(course, userContext))
                .sorted(Comparator.comparing((TeachingCourse course) -> course.updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(course -> course.name == null ? "~" : course.name))
                .toList();
    }

    public TeachingCourse create(RequestUserContext userContext, CourseCreateRequest request) {
        authorizationService.requireTeachingManager(userContext, "只有教师或管理员可以创建课程");
        TeachingCourse course = new TeachingCourse();
        course.courseId = nextId();
        course.name = request.name().trim();
        course.code = blankToNull(request.code());
        course.description = blankToNull(request.description());
        course.ownerId = userContext.userId();
        course.ownerName = firstNonBlank(userContext.userName(), userContext.userId());
        course.orgUnitId = blankToNull(userContext.orgUnitId());
        course.archived = false;
        course.createdAt = Instant.now();
        course.updatedAt = course.createdAt;

        TeachingCourse saved = teachingCourseRepository.save(course);
        auditLogService.record(userContext, "COURSE_CREATED", "COURSE", saved.courseId, Map.of(
                "name", saved.name,
                "code", blankToEmpty(saved.code)
        ));
        return saved;
    }

    public TeachingCourse requireVisible(String courseId, RequestUserContext userContext) {
        TeachingCourse course = teachingCourseRepository.findById(courseId)
                .orElseThrow(() -> new BizException(404, "课程不存在"));
        if (!authorizationService.canViewCourse(course, userContext)) {
            throw new BizException(403, "无权访问该课程");
        }
        return course;
    }

    public TeachingCourse requireManageable(String courseId, RequestUserContext userContext) {
        TeachingCourse course = requireVisible(courseId, userContext);
        if (!authorizationService.canManageCourse(course, userContext)) {
            throw new BizException(403, "当前仅支持课程创建者或管理员管理课程作业");
        }
        return course;
    }

    public TeachingCourse updateArchiveStatus(String courseId, RequestUserContext userContext, boolean archived) {
        TeachingCourse course = requireManageable(courseId, userContext);
        if (course.archived == archived) {
            return course;
        }
        course.archived = archived;
        course.updatedAt = Instant.now();
        TeachingCourse saved = teachingCourseRepository.save(course);
        auditLogService.record(userContext, archived ? "COURSE_ARCHIVED" : "COURSE_UNARCHIVED", "COURSE", saved.courseId, Map.of(
                "name", saved.name,
                "archived", saved.archived
        ));
        return saved;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String first, String second) {
        String a = blankToNull(first);
        if (a != null) {
            return a;
        }
        return blankToNull(second);
    }

    private String nextId() {
        return "course-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
