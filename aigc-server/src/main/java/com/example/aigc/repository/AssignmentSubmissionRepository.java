package com.example.aigc.repository;

import com.example.aigc.entity.AssignmentSubmission;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository {
    AssignmentSubmission save(AssignmentSubmission submission);

    Optional<AssignmentSubmission> findById(String submissionId);

    List<AssignmentSubmission> findAllByAssignmentId(String assignmentId);

    List<AssignmentSubmission> findAllByProjectId(String projectId);
}
