package com.example.aigc.repository.jpa;

import com.example.aigc.entity.OrgUnit;
import com.example.aigc.repository.OrgUnitRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaOrgUnitRepository implements OrgUnitRepository {
    private final SpringDataOrgUnitRepository repository;

    public JpaOrgUnitRepository(SpringDataOrgUnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrgUnit save(OrgUnit unit) {
        return repository.save(unit);
    }

    @Override
    public Optional<OrgUnit> findById(String unitId) {
        return repository.findById(unitId);
    }

    @Override
    public List<OrgUnit> findAll() {
        return repository.findAll();
    }
}
