package com.example.aigc.service;

import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.repository.StoredFileRecordRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MediaResourceService {
    private final StoredFileRecordRepository storedFileRecordRepository;
    private final AuthorizationService authorizationService;

    public MediaResourceService(
            StoredFileRecordRepository storedFileRecordRepository,
            AuthorizationService authorizationService
    ) {
        this.storedFileRecordRepository = storedFileRecordRepository;
        this.authorizationService = authorizationService;
    }

    public List<StoredFileRecord> listRecent(RequestUserContext actor) {
        authorizationService.requireTeachingManager(actor, "只有教师或管理员可以查看媒体资源目录");
        return storedFileRecordRepository.findRecent(200).stream()
                .sorted(Comparator.comparing((StoredFileRecord item) -> item.createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}
