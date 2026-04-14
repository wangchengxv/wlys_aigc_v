package com.example.aigc.service;

import com.example.aigc.dto.PromptVersion;
import com.example.aigc.enums.PromptVersionSource;
import com.example.aigc.exception.BizException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PromptVersionService {

    private static final int DEFAULT_MAX_ENTRIES = 30;

    private static String normalize(String prompt) {
        return prompt == null ? "" : prompt.trim();
    }

    public List<PromptVersion> updateWithVersion(
            String currentPrompt,
            String nextPrompt,
            List<PromptVersion> versions,
            PromptVersionSource source,
            String note
    ) {
        return updateWithVersion(currentPrompt, nextPrompt, versions, source, note, DEFAULT_MAX_ENTRIES);
    }

    /**
     * 对齐 BigBanana promptVersionService：空历史且当前有值时补一条 Initial snapshot；与上一条相同则跳过追加。
     */
    public List<PromptVersion> updateWithVersion(
            String currentPrompt,
            String nextPrompt,
            List<PromptVersion> versions,
            PromptVersionSource source,
            String note,
            int maxEntries
    ) {
        List<PromptVersion> nextVersions = versions == null || versions.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(versions);
        String normalizedCurrent = normalize(currentPrompt);
        String normalizedNext = normalize(nextPrompt);

        if (nextVersions.isEmpty() && !normalizedCurrent.isEmpty()) {
            nextVersions = appendPromptVersion(nextVersions, normalizedCurrent, PromptVersionSource.IMPORTED, "Initial snapshot", maxEntries);
        }
        if (normalizedNext.isEmpty()) {
            return nextVersions;
        }
        return appendPromptVersion(nextVersions, normalizedNext, source, note, maxEntries);
    }

    public List<PromptVersion> appendPromptVersion(
            List<PromptVersion> versions,
            String prompt,
            PromptVersionSource source,
            String note,
            int maxEntries
    ) {
        String normalized = normalize(prompt);
        if (normalized.isEmpty()) {
            return versions == null ? new ArrayList<>() : new ArrayList<>(versions);
        }
        List<PromptVersion> next = versions == null ? new ArrayList<>() : new ArrayList<>(versions);
        PromptVersion last = next.isEmpty() ? null : next.get(next.size() - 1);
        if (last != null && normalize(last.prompt()).equals(normalized)) {
            return next;
        }
        next.add(new PromptVersion(
                "pv-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase(Locale.ROOT),
                normalized,
                System.currentTimeMillis(),
                source,
                note
        ));
        if (next.size() > maxEntries) {
            return new ArrayList<>(next.subList(next.size() - maxEntries, next.size()));
        }
        return next;
    }

    public PromptVersion findVersion(List<PromptVersion> versions, String versionId) {
        if (versions == null || versionId == null || versionId.isBlank()) {
            return null;
        }
        for (PromptVersion v : versions) {
            if (versionId.equals(v.id())) {
                return v;
            }
        }
        return null;
    }

    /**
     * 回滚：将当前字段设为选中版本内容，并追加一条 rollback 记录。
     */
    public List<PromptVersion> rollbackAppend(
            String currentPrompt,
            List<PromptVersion> versions,
            String versionId
    ) {
        PromptVersion target = findVersion(versions, versionId);
        if (target == null) {
            throw new BizException(400, "未找到指定版本");
        }
        return updateWithVersion(currentPrompt, target.prompt(), versions, PromptVersionSource.ROLLBACK, null);
    }
}
