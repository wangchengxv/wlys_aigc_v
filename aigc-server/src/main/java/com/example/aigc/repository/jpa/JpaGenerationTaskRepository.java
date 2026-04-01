package com.example.aigc.repository.jpa;

import com.example.aigc.entity.GenerationTask;
import com.example.aigc.enums.GenerateMode;
import com.example.aigc.repository.GenerationTaskRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaGenerationTaskRepository implements GenerationTaskRepository {
    private final SpringDataGenerationTaskRepository delegate;

    public JpaGenerationTaskRepository(SpringDataGenerationTaskRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(GenerationTask task) {
        delegate.save(task);
    }

    @Override
    public Optional<GenerationTask> findByTaskId(String taskId) {
        return delegate.findById(taskId);
    }

    @Override
    public List<GenerationTask> page(int page, int pageSize, GenerateMode mode, String ownerId) {
        int safePage = Math.max(page - 1, 0);
        List<GenerationTask> all;
        if (mode == null && ownerId == null) {
            all = delegate.findAll();
            all = all.stream().sorted(java.util.Comparator.comparing(GenerationTask::getCreatedAt).reversed()).toList();
        } else if (mode == null) {
            all = delegate.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        } else if (ownerId == null) {
            all = delegate.findAllByModeOrderByCreatedAtDesc(mode);
        } else {
            all = delegate.findAllByOwnerIdAndModeOrderByCreatedAtDesc(ownerId, mode);
        }
        int from = Math.max(0, safePage * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        if (from >= all.size()) {
            return List.of();
        }
        return all.subList(from, to);
    }

    @Override
    public long count(GenerateMode mode, String ownerId) {
        if (mode == null && ownerId == null) {
            return delegate.count();
        }
        if (mode == null) {
            return delegate.countByOwnerId(ownerId);
        }
        if (ownerId == null) {
            return delegate.countByMode(mode);
        }
        return delegate.countByOwnerIdAndMode(ownerId, mode);
    }

    @Override
    public void deleteByTaskId(String taskId) {
        delegate.deleteById(taskId);
    }
}
