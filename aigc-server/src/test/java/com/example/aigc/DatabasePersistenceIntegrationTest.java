package com.example.aigc;

import com.example.aigc.entity.ScriptProject;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptProjectSummary;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.ScriptProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatabasePersistenceIntegrationTest {
    private static final Path DATA_DIR = createDataDir();

    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;

    @Autowired
    private ScriptProjectRepository scriptProjectRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("aigc.storage.data-dir", () -> DATA_DIR.toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-db-it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Test
    void repositoriesPersistAndReadFromDatabase() {
        ConnectionConfig config = ConnectionConfig.create("Test", "openai", "https://api.openai.com/v1", "enc-key", true);
        connectionConfigRepository.save(config);
        assertThat(connectionConfigRepository.findById(config.getId())).isPresent();

        ScriptProjectAggregate aggregate = new ScriptProjectAggregate();
        ScriptProject project = new ScriptProject();
        project.projectId = "sp-test-1";
        project.name = "db-test";
        project.status = ProjectStatus.DRAFT;
        project.createdAt = Instant.now();
        project.updatedAt = project.createdAt;
        aggregate.project = project;
        scriptProjectRepository.save(aggregate);

        assertThat(scriptProjectRepository.findById(project.projectId)).isPresent();
        List<ScriptProjectSummary> summaries = scriptProjectRepository.findAll(false);
        assertThat(summaries).anyMatch(item -> "sp-test-1".equals(item.projectId));
    }

    private static Path createDataDir() {
        try {
            return Files.createTempDirectory("aigc-db-it-");
        } catch (Exception ex) {
            throw new IllegalStateException("创建测试目录失败", ex);
        }
    }
}
