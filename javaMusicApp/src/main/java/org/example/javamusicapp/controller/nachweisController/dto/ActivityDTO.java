package org.example.javamusicapp.controller.nachweisController.dto;

import lombok.Data;
import org.example.javamusicapp.model.enums.Weekday;

import java.math.BigDecimal;

@Data
public class ActivityDTO {
    private Weekday day;
    private Integer slot;
    private String description;
    private BigDecimal hours;
    private String section;
}
