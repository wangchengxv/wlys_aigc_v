package com.example.aigc.repository.jpa;

import com.example.aigc.entity.AssignmentSubmission;
import com.example.aigc.repository.AssignmentSubmissionRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaAssignmentSubmissionRepository implements AssignmentSubmissionRepository {
    private final SpringDataAssignmentSubmissionRepository repository;

    public JpaAssignmentSubmissionRepository(SpringDataAssignmentSubmissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public AssignmentSubmission save(AssignmentSubmission submission) {
        return repository.save(submission);
    }

    @Override
    public Optional<AssignmentSubmission> findById(String submissionId) {
        return repository.findById(submissionId);
    }

    @Override
    public List<AssignmentSubmission> findAllById(List<String> submissionIds) {
        return repository.findAllById(submissionIds);
    }

    @Override
    public List<AssignmentSubmission> findAllByAssignmentId(String assignmentId) {
        return repository.findAllByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
    }

    @Override
    public List<AssignmentSubmission> findAllByProjectId(String projectId) {
        return repository.findAllByProjectId(projectId);
    }
}
