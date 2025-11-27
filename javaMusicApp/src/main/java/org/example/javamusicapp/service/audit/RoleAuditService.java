package org.example.javamusicapp.service.audit;

import org.example.javamusicapp.model.RoleAudit;
import org.example.javamusicapp.repository.RoleAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 📝 **Was geht hier ab?**
 * Dieser Service ist das Gehirn hinter dem Rollen-Audit. Hier läuft die Business-Logik,
 * wenn es um das Nachverfolgen von Rollen-Änderungen geht.
 *
 * Seine Hauptaufgaben:
 * - **record()**: Wird aufgerufen, wenn jemand einem User eine Rolle gibt oder wegnimmt.
 *   Der Service erstellt dann einen `RoleAudit`-Eintrag in der Datenbank mit allen wichtigen
 *   Infos: Wer hat's getan, bei wem wurde was geändert und wann.
 * - **list()**: Holt alle Audit-Einträge aus der Datenbank, schön sortiert und aufgeteilt
 *   in Seiten (paginated), damit der `RoleAuditController` sie anzeigen kann.
 */
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

    public Page<RoleAudit> list(Pageable pageable) {
        // Ensure sort by performedAt desc unless caller provided a sort
        Pageable effective = pageable;
        if (pageable.getSort().isUnsorted()) {
            effective = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC,
                            "performedAt"));
        }
        return repository.findAll(effective);
    }
}
