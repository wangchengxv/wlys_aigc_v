package com.example.aigc.repository.jpa;

import com.example.aigc.entity.AccountImportTask;
import com.example.aigc.repository.AccountImportTaskRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaAccountImportTaskRepository implements AccountImportTaskRepository {
    private final SpringDataAccountImportTaskRepository repository;

    public JpaAccountImportTaskRepository(SpringDataAccountImportTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public AccountImportTask save(AccountImportTask task) {
        return repository.save(task);
    }

    @Override
    public Optional<AccountImportTask> findById(String taskId) {
        return repository.findById(taskId);
    }

    @Override
    public List<AccountImportTask> findRecent(int limit) {
        List<AccountImportTask> tasks = repository.findTop100ByOrderByCreatedAtDesc();
        if (limit <= 0 || tasks.size() <= limit) {
            return tasks;
        }
        return tasks.subList(0, limit);
    }
}
