package org.example.javamusicapp.repository;

import org.example.javamusicapp.model.NachweisAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NachweisAuditLogRepository extends JpaRepository<NachweisAuditLog, Long> {
}
