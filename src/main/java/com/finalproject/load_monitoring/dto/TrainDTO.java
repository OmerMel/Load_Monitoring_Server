package com.finalproject.load_monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainDTO {

    private Long trainId;
    private String originStation;
    private String destinationStation;
    private String departureTime;
    private String arrivalTime;
    private String lastUpdated;
    private List<CarriageDTO> carriages;
}
