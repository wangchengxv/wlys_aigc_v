package com.miioo.backend.service.orchestration;

import com.miioo.backend.asset.AssetEntity;
import com.miioo.backend.asset.AssetMapper;
import com.miioo.backend.integration.ai.ProviderRouter;
import com.miioo.backend.script.ScriptEntity;
import com.miioo.backend.script.ScriptMapper;
import com.miioo.backend.service.aitask.AiTaskService;
import com.miioo.backend.subject.SubjectEntity;
import com.miioo.backend.subject.SubjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OrchestrationService {
    private final ProviderRouter providerRouter;
    private final AiTaskService aiTaskService;
    private final ScriptMapper scriptMapper;
    private final SubjectMapper subjectMapper;
    private final AssetMapper assetMapper;

    public OrchestrationService(ProviderRouter providerRouter,
                                AiTaskService aiTaskService,
                                ScriptMapper scriptMapper,
                                SubjectMapper subjectMapper,
                                AssetMapper assetMapper) {
        this.providerRouter = providerRouter;
        this.aiTaskService = aiTaskService;
        this.scriptMapper = scriptMapper;
        this.subjectMapper = subjectMapper;
        this.assetMapper = assetMapper;
    }

    public Long generateScriptTask(Long userId, Long projectId, String prompt) {
        Long taskId = aiTaskService.createTask(userId, "SCRIPT_GENERATE", projectId);
        aiTaskService.runMockTask(taskId, () -> {
            var aiResult = providerRouter.provider().generateScript(prompt);
            ScriptEntity item = new ScriptEntity();
            item.setProjectId(projectId);
            item.setTitle(String.valueOf(aiResult.getOrDefault("title", "Mock Script")));
            item.setContent(String.valueOf(aiResult.getOrDefault("content", "")));
            scriptMapper.insert(item);
        });
        return taskId;
    }

    public Long extractSubjectTask(Long userId, Long projectId, String scriptContent) {
        Long taskId = aiTaskService.createTask(userId, "SUBJECT_EXTRACT", projectId);
        aiTaskService.runMockTask(taskId, () -> {
            var aiResult = providerRouter.provider().extractSubjects(scriptContent);
            SubjectEntity item = new SubjectEntity();
            item.setProjectId(projectId);
            item.setName("主角A");
            item.setSubjectType("CHARACTER");
            item.setFinalized(Boolean.FALSE);
            if (aiResult.containsKey("characters")) {
                Object characters = aiResult.get("characters");
                if (characters instanceof java.util.List<?> list && !list.isEmpty()) {
                    item.setName(String.valueOf(list.get(0)));
                }
            }
            subjectMapper.insert(item);
        });
        return taskId;
    }

    public Long generateImageTask(Long userId, Long projectId, String prompt) {
        Long taskId = aiTaskService.createTask(userId, "IMAGE_GENERATE", projectId);
        aiTaskService.runMockTask(taskId, () -> {
            var aiResult = providerRouter.provider().generateImage(prompt);
            AssetEntity item = new AssetEntity();
            item.setProjectId(projectId);
            item.setAssetType("IMAGE");
            item.setUrl(String.valueOf(aiResult.getOrDefault("url", "")));
            item.setStarred(Boolean.FALSE);
            assetMapper.insert(item);
        });
        return taskId;
    }
}
