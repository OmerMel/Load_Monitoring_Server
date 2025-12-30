package com.finalproject.load_monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarriageDTO {

    private Long carriageId;
    private Long trainId;
    private int carriageNumber;
    private int occupancy;
    private int maxCapacity;
    private String lastUpdated;
}
