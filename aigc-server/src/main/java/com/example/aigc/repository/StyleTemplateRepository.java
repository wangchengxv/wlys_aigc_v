package com.example.aigc.repository;

import com.example.aigc.entity.StyleTemplate;

import java.util.List;
import java.util.Optional;

public interface StyleTemplateRepository {
    StyleTemplate save(StyleTemplate template);

    Optional<StyleTemplate> findById(String templateId);

    List<StyleTemplate> findAll();
}
