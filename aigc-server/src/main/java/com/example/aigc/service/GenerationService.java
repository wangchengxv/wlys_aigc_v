package com.example.aigc.service;

import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.enums.GenerateMode;

public interface GenerationService {
    GenerateResponseData generate(GenerateRequest request, String ownerId);

    PagedResult<GenerateResponseData> history(int page, int pageSize, GenerateMode mode, String ownerId);

    GenerateResponseData taskDetail(String taskId, String ownerId);

    void deleteTask(String taskId, String ownerId);
}
