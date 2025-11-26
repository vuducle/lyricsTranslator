package org.example.javamusicapp.service.audit;

import org.example.javamusicapp.model.RoleAudit;
import org.example.javamusicapp.repository.RoleAuditRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoleAuditService {
    private final RoleAuditRepository repository;

    public RoleAuditService(RoleAuditRepository repository) {
        this.repository = repository;
    }

    public void record(String action, String targetUsername, String performedBy, String details) {
        RoleAudit audit = RoleAudit.builder()
                .action(action)
                .targetUsername(targetUsername)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .details(details)
                .build();
        repository.save(audit);
    }

    public org.springframework.data.domain.Page<RoleAudit> list(org.springframework.data.domain.Pageable pageable) {
        // Ensure sort by performedAt desc unless caller provided a sort
        org.springframework.data.domain.Pageable effective = pageable;
        if (pageable.getSort().isUnsorted()) {
            effective = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                            "performedAt"));
        }
        return repository.findAll(effective);
    }
}
