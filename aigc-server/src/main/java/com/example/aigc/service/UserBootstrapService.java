package com.example.aigc.service;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.entity.AppUser;
import com.example.aigc.entity.OrgUnit;
import com.example.aigc.enums.OrgUnitType;
import com.example.aigc.repository.AppUserRepository;
import com.example.aigc.repository.OrgUnitRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserBootstrapService implements ApplicationRunner {
    private final AuthProperties authProperties;
    private final AppUserRepository appUserRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PasswordCodec passwordCodec;

    public UserBootstrapService(
            AuthProperties authProperties,
            AppUserRepository appUserRepository,
            OrgUnitRepository orgUnitRepository,
            PasswordCodec passwordCodec
    ) {
        this.authProperties = authProperties;
        this.appUserRepository = appUserRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.passwordCodec = passwordCodec;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (AuthProperties.SeedUser seedUser : authProperties.getSeedUsers()) {
            if (seedUser.getUserId() == null || seedUser.getUserId().isBlank()
                    || seedUser.getUsername() == null || seedUser.getUsername().isBlank()
                    || seedUser.getPassword() == null || seedUser.getPassword().isBlank()) {
                continue;
            }
            AppUser user = appUserRepository.findById(seedUser.getUserId())
                    .or(() -> appUserRepository.findByUsername(seedUser.getUsername()))
                    .orElseGet(AppUser::new);
            ensureOrgUnits(seedUser);
            boolean isNew = user.userId == null || user.userId.isBlank();
            user.userId = seedUser.getUserId().trim();
            user.username = seedUser.getUsername().trim();
            user.displayName = blankToNull(seedUser.getDisplayName());
            user.role = seedUser.getRole();
            user.orgUnitId = blankToNull(seedUser.getOrgUnitId());
            user.classroomId = blankToNull(seedUser.getClassroomId());
            user.enabled = seedUser.isEnabled();
            if (isNew) {
                user.locked = false;
                user.lockReason = null;
                user.lockedAt = null;
                user.failedLoginCount = 0;
                user.forcePasswordChange = false;
                user.sessionVersion = 0;
            }
            if (isNew || user.passwordHash == null || user.passwordHash.isBlank()) {
                user.passwordHash = passwordCodec.encode(seedUser.getPassword().trim());
                user.createdAt = Instant.now();
                user.passwordUpdatedAt = user.createdAt;
            }
            user.updatedAt = Instant.now();
            appUserRepository.save(user);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void ensureOrgUnits(AuthProperties.SeedUser seedUser) {
        String orgUnitId = blankToNull(seedUser.getOrgUnitId());
        if (orgUnitId != null && orgUnitRepository.findById(orgUnitId).isEmpty()) {
            OrgUnit orgUnit = new OrgUnit();
            orgUnit.unitId = orgUnitId;
            orgUnit.name = orgUnitId;
            orgUnit.type = OrgUnitType.ORGANIZATION;
            orgUnit.createdAt = Instant.now();
            orgUnit.updatedAt = orgUnit.createdAt;
            orgUnitRepository.save(orgUnit);
        }
        String classroomId = blankToNull(seedUser.getClassroomId());
        if (classroomId != null && orgUnitRepository.findById(classroomId).isEmpty()) {
            OrgUnit classroom = new OrgUnit();
            classroom.unitId = classroomId;
            classroom.name = classroomId;
            classroom.type = OrgUnitType.CLASSROOM;
            classroom.parentUnitId = orgUnitId;
            classroom.createdAt = Instant.now();
            classroom.updatedAt = classroom.createdAt;
            orgUnitRepository.save(classroom);
        }
    }
}
