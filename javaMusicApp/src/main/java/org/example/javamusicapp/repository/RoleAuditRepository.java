package org.example.javamusicapp.repository;

import org.example.javamusicapp.model.RoleAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleAuditRepository extends JpaRepository<RoleAudit, Long> {

}
