package org.example.javamusicapp.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.model.RoleAudit;
import org.example.javamusicapp.model.enums.ERole;
import org.example.javamusicapp.service.audit.RoleAuditService;
import org.example.javamusicapp.controller.admin.dto.RoleAuditDto;
import org.example.javamusicapp.controller.admin.dto.RoleAuditPageWrapper;
import org.example.javamusicapp.controller.nachweisController.dto.AuditPageResponse;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import org.example.javamusicapp.service.nachweis.NachweisSecurityService;
import org.example.javamusicapp.service.auth.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin Werkzeuge")
@SecurityRequirement(name = "bearerAuth")
public class RoleAuditController {

    private final RoleAuditService roleAuditService;
    private final NachweisSecurityService nachweisSecurityService;
    private final UserService userService;

    @Operation(summary = "Rollen-Audit", description = "Listet Einträge zu Rollen-Zuweisungen und -Entfernungen")
    @GetMapping("/rollen-audit")
    @PreAuthorize("hasRole('ADMIN') or @nachweisSecurityService.isAusbilder(authentication)")
    public ResponseEntity<RoleAuditPageWrapper> listRoleAudits(Authentication authentication,
               @RequestParam(value = "page", defaultValue = "0") int page,
                @RequestParam(value = "size", defaultValue = "50") int size)
    {
        Pageable pageable = PageRequest.of(page, size);
        Page<RoleAudit> audits = roleAuditService.list(pageable);

        // map to DTOs
        List<RoleAuditDto> items = audits.getContent().stream().map(a -> RoleAuditDto.builder()
                .id(a.getId())
                .action(a.getAction())
                .targetUsername(a.getTargetUsername())
                .performedBy(a.getPerformedBy())
                .performedAt(a.getPerformedAt())
                .details(a.getDetails())
                .build()).collect(Collectors.toList());

        AuditPageResponse<RoleAuditDto> resp = new AuditPageResponse<>(items, audits.getNumber(), audits.getSize(), audits.getTotalPages(), audits.getTotalElements());

        // add metadata: visible groups and lists
        List<String> sichtbareGruppen = new ArrayList<>();
        sichtbareGruppen.add("Users (Azubis)");
        sichtbareGruppen.add("Admins (Ausbilder)");

        List<String> azubis = new ArrayList<>();
        List<String> ausbilder = new ArrayList<>();
        try {
            azubis = userService.listUsernamesByRole(ERole.ROLE_USER);
            ausbilder = userService.listUsernamesByRole(ERole.ROLE_ADMIN);
        } catch (Exception e) {
            log.warn("Konnte Benutzerlisten für Audit nicht laden: {}", e.getMessage());
        }

        RoleAuditPageWrapper wrapperDto = new RoleAuditPageWrapper(resp, sichtbareGruppen, azubis, ausbilder);
        return ResponseEntity.ok(wrapperDto);
    }
}
