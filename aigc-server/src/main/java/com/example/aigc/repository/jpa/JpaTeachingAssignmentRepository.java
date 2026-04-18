package com.example.aigc.repository.jpa;

import com.example.aigc.entity.TeachingAssignment;
import com.example.aigc.repository.TeachingAssignmentRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaTeachingAssignmentRepository implements TeachingAssignmentRepository {
    private final SpringDataTeachingAssignmentRepository repository;

    public JpaTeachingAssignmentRepository(SpringDataTeachingAssignmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public TeachingAssignment save(TeachingAssignment assignment) {
        return repository.save(assignment);
    }

    @Override
    public Optional<TeachingAssignment> findById(String assignmentId) {
        return repository.findById(assignmentId);
    }

    @Override
    public List<TeachingAssignment> findAllByCourseId(String courseId) {
        return repository.findAllByCourseIdOrderByCreatedAtDesc(courseId);
    }
}
