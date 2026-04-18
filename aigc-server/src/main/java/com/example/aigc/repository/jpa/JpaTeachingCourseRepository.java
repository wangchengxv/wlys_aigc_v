package com.example.aigc.repository.jpa;

import com.example.aigc.entity.TeachingCourse;
import com.example.aigc.repository.TeachingCourseRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaTeachingCourseRepository implements TeachingCourseRepository {
    private final SpringDataTeachingCourseRepository repository;

    public JpaTeachingCourseRepository(SpringDataTeachingCourseRepository repository) {
        this.repository = repository;
    }

    @Override
    public TeachingCourse save(TeachingCourse course) {
        return repository.save(course);
    }

    @Override
    public Optional<TeachingCourse> findById(String courseId) {
        return repository.findById(courseId);
    }

    @Override
    public List<TeachingCourse> findAll() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }
}
