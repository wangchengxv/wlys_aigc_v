package com.example.aigc.service;

import com.example.aigc.dto.*;
import com.example.aigc.entity.AssignmentSubmission;
import com.example.aigc.entity.ReviewRecord;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.TeachingAssignment;
import com.example.aigc.entity.TeachingCourse;
import com.example.aigc.enums.AssignmentStatus;
import com.example.aigc.enums.SubmissionStatus;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AssignmentSubmissionRepository;
import com.example.aigc.repository.ReviewRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeachingSubmissionService {
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final TeachingAssignmentService teachingAssignmentService;
    private final TeachingCourseService teachingCourseService;
    private final ScriptProjectService scriptProjectService;
    private final AuditLogService auditLogService;
    private final AuthorizationService authorizationService;

    public TeachingSubmissionService(
            AssignmentSubmissionRepository assignmentSubmissionRepository,
            ReviewRecordRepository reviewRecordRepository,
            TeachingAssignmentService teachingAssignmentService,
            TeachingCourseService teachingCourseService,
            ScriptProjectService scriptProjectService,
            AuditLogService auditLogService,
            AuthorizationService authorizationService
    ) {
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.teachingAssignmentService = teachingAssignmentService;
        this.teachingCourseService = teachingCourseService;
        this.scriptProjectService = scriptProjectService;
        this.auditLogService = auditLogService;
        this.authorizationService = authorizationService;
    }

    public List<AssignmentSubmission> listByAssignment(String assignmentId, RequestUserContext userContext) {
        TeachingAssignment assignment = teachingAssignmentService.requireVisible(assignmentId, userContext);
        return assignmentSubmissionRepository.findAllByAssignmentId(assignmentId).stream()
                .filter(item -> authorizationService.canViewSubmission(assignment, item, userContext))
                .sorted(Comparator.comparing((AssignmentSubmission item) -> item.submittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public AssignmentSubmission create(String assignmentId, RequestUserContext userContext, SubmissionCreateRequest request) {
        TeachingAssignment assignment = teachingAssignmentService.requireVisible(assignmentId, userContext);
        TeachingCourse course = teachingCourseService.requireVisible(assignment.courseId, userContext);
        if (course.archived) {
            throw new BizException(400, "课程已归档，不能继续提交作业");
        }
        if (assignment.status == AssignmentStatus.CLOSED) {
            throw new BizException(400, "作业已关闭，不能再提交");
        }
        if (assignment.dueAt != null && !Instant.now().isBefore(assignment.dueAt)) {
            throw new BizException(400, "作业已截止，不能再提交");
        }

        scriptProjectService.assertProjectAccess(request.projectId(), userContext, false);
        ScriptProjectAggregate projectAggregate = scriptProjectService.require(request.projectId(), userContext);
        String projectCourseId = blankToNull(projectAggregate.project.courseId);
        if (projectCourseId != null && !Objects.equals(projectCourseId, assignment.courseId)) {
            throw new BizException(400, "项目绑定课程与当前作业不一致");
        }
        String requiredStyleTemplateId = blankToNull(assignment.styleTemplateId);
        String projectStyleTemplateId = blankToNull(projectAggregate.project.styleTemplateId);
        if (requiredStyleTemplateId != null && !Objects.equals(projectStyleTemplateId, requiredStyleTemplateId)) {
            throw new BizException(400, "项目未绑定作业要求的风格模板");
        }

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.submissionId = nextSubmissionId();
        submission.assignmentId = assignment.assignmentId;
        submission.courseId = assignment.courseId;
        submission.projectId = request.projectId().trim();
        submission.studentUserId = userContext.userId();
        submission.studentUserName = firstNonBlank(userContext.userName(), userContext.userId());
        submission.note = blankToNull(request.note());
        submission.status = SubmissionStatus.SUBMITTED;
        submission.submittedAt = Instant.now();
        submission.createdAt = submission.submittedAt;
        submission.updatedAt = submission.submittedAt;

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
        auditLogService.record(userContext, "ASSIGNMENT_SUBMITTED", "SUBMISSION", saved.submissionId, Map.of(
                "assignmentId", saved.assignmentId,
                "projectId", saved.projectId
        ));
        return saved;
    }

    public AssignmentSubmission review(String submissionId, RequestUserContext userContext, SubmissionReviewRequest request) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new BizException(404, "提交记录不存在"));
        TeachingAssignment assignment = teachingAssignmentService.requireReviewable(submission.assignmentId, userContext);
        if (request.status() == SubmissionStatus.SUBMITTED) {
            throw new BizException(400, "评审状态不能设置为SUBMITTED");
        }

        submission.status = request.status();
        submission.score = request.score();
        submission.reviewComment = blankToNull(request.comment());
        submission.reviewedAt = Instant.now();
        submission.updatedAt = submission.reviewedAt;
        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);

        ReviewRecord record = new ReviewRecord();
        record.reviewId = nextReviewId();
        record.submissionId = saved.submissionId;
        record.assignmentId = assignment.assignmentId;
        record.reviewerUserId = userContext.userId();
        record.reviewerUserName = firstNonBlank(userContext.userName(), userContext.userId());
        record.status = request.status();
        record.score = request.score();
        record.comment = blankToNull(request.comment());
        record.createdAt = Instant.now();
        reviewRecordRepository.save(record);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("assignmentId", saved.assignmentId);
        details.put("status", saved.status.name());
        details.put("score", saved.score);
        auditLogService.record(userContext, "SUBMISSION_REVIEWED", "SUBMISSION", saved.submissionId, details);
        return saved;
    }

    @Transactional
    public BatchReviewSubmissionsResponse batchReview(String assignmentId, RequestUserContext userContext, BatchReviewSubmissionsRequest request) {
        TeachingAssignment assignment = teachingAssignmentService.requireReviewable(assignmentId, userContext);
        if (request.status() == SubmissionStatus.SUBMITTED) {
            throw new BizException(400, "评审状态不能设置为SUBMITTED");
        }

        List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findAllById(request.submissionIds());
        List<BatchReviewSubmissionsResponse.FailedItem> failedItems = new ArrayList<>();
        int successCount = 0;

        for (String submissionId : request.submissionIds()) {
            try {
                Optional<AssignmentSubmission> opt = submissions.stream()
                        .filter(s -> s.submissionId.equals(submissionId))
                        .findFirst();

                if (opt.isEmpty()) {
                    failedItems.add(new BatchReviewSubmissionsResponse.FailedItem(submissionId, "提交记录不存在"));
                    continue;
                }

                AssignmentSubmission submission = opt.get();
                if (!submission.assignmentId.equals(assignmentId)) {
                    failedItems.add(new BatchReviewSubmissionsResponse.FailedItem(submissionId, "该提交不属于当前作业"));
                    continue;
                }

                submission.status = request.status();
                submission.score = request.score();
                submission.reviewComment = blankToNull(request.comment());
                submission.reviewedAt = Instant.now();
                submission.updatedAt = submission.reviewedAt;
                assignmentSubmissionRepository.save(submission);

                ReviewRecord record = new ReviewRecord();
                record.reviewId = nextReviewId();
                record.submissionId = submission.submissionId;
                record.assignmentId = assignment.assignmentId;
                record.reviewerUserId = userContext.userId();
                record.reviewerUserName = firstNonBlank(userContext.userName(), userContext.userId());
                record.status = request.status();
                record.score = request.score();
                record.comment = blankToNull(request.comment());
                record.createdAt = Instant.now();
                reviewRecordRepository.save(record);

                Map<String, Object> details = new LinkedHashMap<>();
                details.put("assignmentId", submission.assignmentId);
                details.put("status", submission.status.name());
                details.put("score", submission.score);
                auditLogService.record(userContext, "SUBMISSION_REVIEWED", "SUBMISSION", submission.submissionId, details);

                successCount++;
            } catch (Exception e) {
                failedItems.add(new BatchReviewSubmissionsResponse.FailedItem(submissionId, e.getMessage()));
            }
        }

        return new BatchReviewSubmissionsResponse(
                request.submissionIds().size(),
                successCount,
                failedItems.size(),
                failedItems
        );
    }

    public AssignmentStatsResponse getAssignmentStats(String assignmentId, RequestUserContext userContext) {
        TeachingAssignment assignment = teachingAssignmentService.requireReviewable(assignmentId, userContext);
        List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findAllByAssignmentId(assignmentId);

        int totalStudents = (int) submissions.stream()
                .map(s -> s.studentUserId)
                .distinct()
                .count();
        int submittedCount = (int) submissions.stream()
                .filter(s -> s.submittedAt != null)
                .count();
        int pendingReviewCount = (int) submissions.stream()
                .filter(s -> s.status == SubmissionStatus.SUBMITTED)
                .count();
        int reviewedCount = (int) submissions.stream()
                .filter(s -> s.status == SubmissionStatus.REVIEWED)
                .count();
        int returnedCount = (int) submissions.stream()
                .filter(s -> s.status == SubmissionStatus.RETURNED)
                .count();

        List<Integer> scores = submissions.stream()
                .filter(s -> s.score != null)
                .map(s -> s.score)
                .toList();

        Double averageScore = scores.isEmpty() ? null : scores.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        int maxScore = scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minScore = scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).min().orElse(0);

        Map<Integer, Integer> scoreDistribution = new LinkedHashMap<>();
        int[] ranges = {0, 60, 70, 80, 90, 100};
        for (int i = 0; i < ranges.length - 1; i++) {
            scoreDistribution.put(ranges[i], 0);
        }

        for (Integer score : scores) {
            if (score < 60) {
                scoreDistribution.merge(0, 1, Integer::sum);
            } else if (score < 70) {
                scoreDistribution.merge(60, 1, Integer::sum);
            } else if (score < 80) {
                scoreDistribution.merge(70, 1, Integer::sum);
            } else if (score < 90) {
                scoreDistribution.merge(80, 1, Integer::sum);
            } else {
                scoreDistribution.merge(90, 1, Integer::sum);
            }
        }

        List<AssignmentStatsResponse.ScoreBucket> scoreBuckets = List.of(
                new AssignmentStatsResponse.ScoreBucket("不及格", 0, 59, scoreDistribution.getOrDefault(0, 0)),
                new AssignmentStatsResponse.ScoreBucket("及格", 60, 69, scoreDistribution.getOrDefault(60, 0)),
                new AssignmentStatsResponse.ScoreBucket("中等", 70, 79, scoreDistribution.getOrDefault(70, 0)),
                new AssignmentStatsResponse.ScoreBucket("良好", 80, 89, scoreDistribution.getOrDefault(80, 0)),
                new AssignmentStatsResponse.ScoreBucket("优秀", 90, 100, scoreDistribution.getOrDefault(90, 0))
        );

        return new AssignmentStatsResponse(
                assignmentId,
                assignment.title,
                totalStudents,
                submittedCount,
                pendingReviewCount,
                reviewedCount,
                returnedCount,
                averageScore,
                maxScore,
                minScore,
                scoreDistribution,
                scoreBuckets
        );
    }

    public List<ReviewRecord> listReviews(String submissionId, RequestUserContext userContext) {
        AssignmentSubmission submission = requireVisibleSubmission(submissionId, userContext);
        return reviewRecordRepository.findAllBySubmissionId(submission.submissionId).stream()
                .sorted(Comparator.comparing((ReviewRecord item) -> item.createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private AssignmentSubmission requireVisibleSubmission(String submissionId, RequestUserContext userContext) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new BizException(404, "提交记录不存在"));
        TeachingAssignment assignment = teachingAssignmentService.requireVisible(submission.assignmentId, userContext);
        if (authorizationService.canViewSubmission(assignment, submission, userContext)) {
            return submission;
        }
        throw new BizException(403, "无权访问该提交记录");
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

    private String nextSubmissionId() {
        return "submission-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String nextReviewId() {
        return "review-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}