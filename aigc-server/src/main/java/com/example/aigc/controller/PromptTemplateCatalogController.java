package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.PromptTemplateCatalogItem;
import com.example.aigc.service.PromptTemplateCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prompt-templates")
public class PromptTemplateCatalogController {

    private final PromptTemplateCatalogService catalogService;

    public PromptTemplateCatalogController(PromptTemplateCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/catalog")
    public ApiResponse<List<PromptTemplateCatalogItem>> catalog() {
        return ApiResponse.ok(catalogService.listCatalog());
    }
}
