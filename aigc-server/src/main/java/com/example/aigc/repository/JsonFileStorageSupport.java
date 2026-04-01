package com.example.aigc.repository;

import com.example.aigc.config.StorageProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Supplier;

@Component
public class JsonFileStorageSupport {

    private final ObjectMapper objectMapper;
    private final Path dataDir;

    public JsonFileStorageSupport(ObjectMapper objectMapper, StorageProperties storageProperties) {
        this.objectMapper = objectMapper;
        this.dataDir = Path.of(storageProperties.getDataDir());
        ensureDirectory();
    }

    public synchronized <T> T readValue(String fileName, TypeReference<T> typeReference, Supplier<T> defaultSupplier) {
        Path path = resolve(fileName);
        return readValue(path, typeReference, defaultSupplier);
    }

    public synchronized <T> T readValue(Path path, TypeReference<T> typeReference, Supplier<T> defaultSupplier) {
        if (!Files.exists(path)) {
            return defaultSupplier.get();
        }
        try {
            return objectMapper.readValue(path.toFile(), typeReference);
        } catch (IOException ex) {
            return defaultSupplier.get();
        }
    }

    public synchronized void writeValue(String fileName, Object value) {
        Path path = resolve(fileName);
        writeValue(path, value);
    }

    public synchronized void writeValue(Path path, Object value) {
        ensureDirectory(path.getParent());
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (IOException ex) {
            throw new IllegalStateException("持久化配置失败: " + path, ex);
        }
    }

    public synchronized void writeString(Path path, String content) {
        ensureDirectory(path.getParent());
        try {
            Files.writeString(path, content == null ? "" : content);
        } catch (IOException ex) {
            throw new IllegalStateException("写入文件失败: " + path, ex);
        }
    }

    public synchronized String readString(Path path, Supplier<String> defaultSupplier) {
        if (!Files.exists(path)) {
            return defaultSupplier.get();
        }
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            return defaultSupplier.get();
        }
    }

    public synchronized void writeBytes(Path path, byte[] content) {
        ensureDirectory(path.getParent());
        try {
            Files.write(path, content);
        } catch (IOException ex) {
            throw new IllegalStateException("写入二进制文件失败: " + path, ex);
        }
    }

    public synchronized void copy(Path source, Path target) {
        ensureDirectory(target.getParent());
        ensureDirectory();
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("复制文件失败: " + source + " -> " + target, ex);
        }
    }

    public Path resolve(String fileName) {
        return dataDir.resolve(fileName);
    }

    public Path getDataDir() {
        ensureDirectory();
        return dataDir;
    }

    public synchronized void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new IllegalStateException("删除目录失败: " + path, ex);
        }
    }

    private void ensureDirectory() {
        ensureDirectory(dataDir);
    }

    private void ensureDirectory(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new IllegalStateException("创建数据目录失败: " + path, ex);
        }
    }
}
