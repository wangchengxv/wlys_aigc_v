package com.example.aigc.repository.jpa;

import com.example.aigc.entity.CanvasGraph;
import com.example.aigc.repository.CanvasGraphRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaCanvasGraphRepository implements CanvasGraphRepository {
    private final SpringDataCanvasGraphRepository repository;

    public JpaCanvasGraphRepository(SpringDataCanvasGraphRepository repository) {
        this.repository = repository;
    }

    @Override
    public CanvasGraph save(CanvasGraph graph) {
        return repository.save(graph);
    }

    @Override
    public Optional<CanvasGraph> findByIdAndOwnerId(String id, String ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId);
    }

    @Override
    public List<CanvasGraph> findAllByOwnerId(String ownerId) {
        return repository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    @Override
    public void deleteByIdAndOwnerId(String id, String ownerId) {
        repository.deleteByIdAndOwnerId(id, ownerId);
    }
}
