package com.example.aigc.repository;

import com.example.aigc.entity.OrgUnit;

import java.util.List;
import java.util.Optional;

public interface OrgUnitRepository {
    OrgUnit save(OrgUnit unit);

    Optional<OrgUnit> findById(String unitId);

    List<OrgUnit> findAll();
}
