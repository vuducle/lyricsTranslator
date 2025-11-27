package org.example.javamusicapp.controller.nachweisController.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditPageResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
}
