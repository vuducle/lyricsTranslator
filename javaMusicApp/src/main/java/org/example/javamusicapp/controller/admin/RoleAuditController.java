package org.example.javamusicapp.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.RoleAudit;
import org.example.javamusicapp.service.audit.RoleAuditService;
import org.example.javamusicapp.service.nachweis.NachweisSecurityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin Werkzeuge")
@SecurityRequirement(name = "bearerAuth")
public class RoleAuditController {

    private final RoleAuditService roleAuditService;
    private final NachweisSecurityService nachweisSecurityService;

    @Operation(summary = "Rollen-Audit", description = "Listet Einträge zu Rollen-Zuweisungen und -Entfernungen")
    @GetMapping("/rollen-audit")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isAusbilder(authentication)")
    public ResponseEntity<java.util.Map<String, Object>> listRoleAudits(Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(value = "page", defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(value = "size", defaultValue = "50") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<RoleAudit> audits = roleAuditService.list(pageable);

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("audits", audits);
        // Sichtbare Gruppen beschreiben (z.B. für UI-Labels)
        java.util.List<String> sichtbareGruppen = new java.util.ArrayList<>();
        sichtbareGruppen.add("Users (Azubis)");
        sichtbareGruppen.add("Admins (Ausbilder)");
        resp.put("sichtbareGruppen", sichtbareGruppen);

        return ResponseEntity.ok(resp);
    }
}
