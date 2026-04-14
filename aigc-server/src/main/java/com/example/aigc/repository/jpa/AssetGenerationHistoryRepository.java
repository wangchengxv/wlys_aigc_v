package com.example.aigc.repository.jpa;

import com.example.aigc.entity.AssetGenerationHistory;
import com.example.aigc.enums.AssetHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetGenerationHistoryRepository extends JpaRepository<AssetGenerationHistory, Long> {
    List<AssetGenerationHistory> findAllByProjectIdOrderByCreatedAtDesc(String projectId);

    List<AssetGenerationHistory> findAllByProjectIdAndAssetTypeOrderByCreatedAtDesc(String projectId, AssetHistoryType assetType);

    List<AssetGenerationHistory> findAllByProjectIdAndAssetTypeAndReferenceIdOrderByCreatedAtDesc(
            String projectId,
            AssetHistoryType assetType,
            String referenceId
    );
}
