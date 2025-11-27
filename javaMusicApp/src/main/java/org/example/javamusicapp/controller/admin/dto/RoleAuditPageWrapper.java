package org.example.javamusicapp.controller.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.javamusicapp.controller.nachweisController.dto.AuditPageResponse;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAuditPageWrapper {
    private AuditPageResponse<RoleAuditDto> audits;
    private List<String> sichtbareGruppen;
    private List<String> azubis;
    private List<String> ausbilder;
}
