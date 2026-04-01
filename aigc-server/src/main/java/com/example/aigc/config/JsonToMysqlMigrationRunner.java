package com.example.aigc.config;

import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.FileConnectionConfigRepository;
import com.example.aigc.repository.FileGenerationTaskRepository;
import com.example.aigc.repository.FileModelConfigRepository;
import com.example.aigc.repository.FileRouterApiKeyRepository;
import com.example.aigc.repository.FileRouterRequestLogRepository;
import com.example.aigc.repository.FileRoutingConfigRepository;
import com.example.aigc.repository.FileScriptProjectRepository;
import com.example.aigc.repository.GenerationTaskRepository;
import com.example.aigc.repository.JsonFileStorageSupport;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.repository.RouterApiKeyRepository;
import com.example.aigc.repository.RouterRequestLogRepository;
import com.example.aigc.repository.RoutingConfigRepository;
import com.example.aigc.repository.ScriptProjectRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

@Component
public class JsonToMysqlMigrationRunner implements ApplicationRunner {
    private static final String MARKER_FILE = ".mysql-migrated.marker";

    private final JsonFileStorageSupport storageSupport;
    private final ConnectionConfigRepository connectionConfigRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final RouterApiKeyRepository routerApiKeyRepository;
    private final RoutingConfigRepository routingConfigRepository;
    private final RouterRequestLogRepository routerRequestLogRepository;
    private final ScriptProjectRepository scriptProjectRepository;

    public JsonToMysqlMigrationRunner(
            JsonFileStorageSupport storageSupport,
            ConnectionConfigRepository connectionConfigRepository,
            ModelConfigRepository modelConfigRepository,
            GenerationTaskRepository generationTaskRepository,
            RouterApiKeyRepository routerApiKeyRepository,
            RoutingConfigRepository routingConfigRepository,
            RouterRequestLogRepository routerRequestLogRepository,
            ScriptProjectRepository scriptProjectRepository
    ) {
        this.storageSupport = storageSupport;
        this.connectionConfigRepository = connectionConfigRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.routerApiKeyRepository = routerApiKeyRepository;
        this.routingConfigRepository = routingConfigRepository;
        this.routerRequestLogRepository = routerRequestLogRepository;
        this.scriptProjectRepository = scriptProjectRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Path marker = storageSupport.getDataDir().resolve(MARKER_FILE);
        String markerValue = storageSupport.readString(marker, () -> "");
        if ("done".equalsIgnoreCase(markerValue)) {
            return;
        }
        if (!shouldMigrate()) {
            storageSupport.writeString(marker, "done");
            return;
        }

        FileConnectionConfigRepository fileConnection = new FileConnectionConfigRepository(storageSupport);
        fileConnection.findAll().forEach(connectionConfigRepository::save);

        FileModelConfigRepository fileModel = new FileModelConfigRepository(storageSupport);
        fileModel.findAll().forEach(modelConfigRepository::save);

        FileGenerationTaskRepository fileTask = new FileGenerationTaskRepository(storageSupport);
        fileTask.page(1, Integer.MAX_VALUE, null, null).forEach(generationTaskRepository::save);

        FileRouterApiKeyRepository fileApiKey = new FileRouterApiKeyRepository(storageSupport);
        fileApiKey.findAll().forEach(routerApiKeyRepository::save);

        FileRoutingConfigRepository fileRouting = new FileRoutingConfigRepository(storageSupport);
        routingConfigRepository.save(fileRouting.get());

        FileRouterRequestLogRepository fileLog = new FileRouterRequestLogRepository(storageSupport);
        fileLog.findAll().forEach(routerRequestLogRepository::save);

        FileScriptProjectRepository fileProject = new FileScriptProjectRepository(storageSupport);
        fileProject.findAll(false).forEach(summary ->
                fileProject.findById(summary.projectId).ifPresent(scriptProjectRepository::save)
        );
        fileProject.findAll(true).forEach(summary ->
                fileProject.findById(summary.projectId).ifPresent(scriptProjectRepository::save)
        );

        storageSupport.writeString(marker, "done");
    }

    private boolean shouldMigrate() {
        return connectionConfigRepository.findAll().isEmpty()
                && modelConfigRepository.findAll().isEmpty()
                && generationTaskRepository.count(null, null) == 0
                && routerApiKeyRepository.findAll().isEmpty()
                && routerRequestLogRepository.findAll().isEmpty()
                && scriptProjectRepository.findAll(false).isEmpty()
                && scriptProjectRepository.findAll(true).isEmpty();
    }
}
