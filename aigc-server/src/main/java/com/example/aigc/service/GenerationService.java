package com.example.aigc.service;

import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.enums.GenerateMode;

public interface GenerationService {
    GenerateResponseData generate(GenerateRequest request);

    PagedResult<GenerateResponseData> history(int page, int pageSize, GenerateMode mode);

    GenerateResponseData taskDetail(String taskId);

    void deleteTask(String taskId);
}
