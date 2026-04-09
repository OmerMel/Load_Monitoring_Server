package com.finalproject.load_monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyLogDTO {
    private Long logId;
    private Long carriageId;
    private int cameraCount;
    private int irCount;
    private int calculatedOccupancy;
    private LocalDateTime timestamp;
}
