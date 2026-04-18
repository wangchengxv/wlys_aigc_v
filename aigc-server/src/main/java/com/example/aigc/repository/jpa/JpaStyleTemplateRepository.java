package com.example.aigc.repository.jpa;

import com.example.aigc.entity.StyleTemplate;
import com.example.aigc.repository.StyleTemplateRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaStyleTemplateRepository implements StyleTemplateRepository {
    private final SpringDataStyleTemplateRepository repository;

    public JpaStyleTemplateRepository(SpringDataStyleTemplateRepository repository) {
        this.repository = repository;
    }

    @Override
    public StyleTemplate save(StyleTemplate template) {
        return repository.save(template);
    }

    @Override
    public Optional<StyleTemplate> findById(String templateId) {
        return repository.findById(templateId);
    }

    @Override
    public List<StyleTemplate> findAll() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }
}
