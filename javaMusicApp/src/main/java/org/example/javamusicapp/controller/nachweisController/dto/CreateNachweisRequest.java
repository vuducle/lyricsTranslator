package org.example.javamusicapp.controller.nachweisController.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateNachweisRequest {
    private LocalDate datumStart;
    private LocalDate datumEnde;
    private int nummer;
    private List<ActivityDTO> activities;
}
