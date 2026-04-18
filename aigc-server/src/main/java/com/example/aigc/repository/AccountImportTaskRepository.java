package com.example.aigc.repository;

import com.example.aigc.entity.AccountImportTask;

import java.util.List;
import java.util.Optional;

public interface AccountImportTaskRepository {
    AccountImportTask save(AccountImportTask task);

    Optional<AccountImportTask> findById(String taskId);

    List<AccountImportTask> findRecent(int limit);
}
