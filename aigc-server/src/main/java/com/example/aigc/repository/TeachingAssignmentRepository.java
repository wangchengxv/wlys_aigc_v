package com.example.aigc.repository;

import com.example.aigc.entity.TeachingAssignment;

import java.util.List;
import java.util.Optional;

public interface TeachingAssignmentRepository {
    TeachingAssignment save(TeachingAssignment assignment);

    Optional<TeachingAssignment> findById(String assignmentId);

    List<TeachingAssignment> findAllByCourseId(String courseId);
}
