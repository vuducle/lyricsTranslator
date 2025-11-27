package org.example.javamusicapp.controller.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAuditDto {
    private Long id;
    private String action;
    private String targetUsername;
    private String performedBy;
    private LocalDateTime performedAt;
    private String details;
}
