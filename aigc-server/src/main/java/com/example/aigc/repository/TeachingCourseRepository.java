package com.example.aigc.repository;

import com.example.aigc.entity.TeachingCourse;

import java.util.List;
import java.util.Optional;

public interface TeachingCourseRepository {
    TeachingCourse save(TeachingCourse course);

    Optional<TeachingCourse> findById(String courseId);

    List<TeachingCourse> findAll();
}
