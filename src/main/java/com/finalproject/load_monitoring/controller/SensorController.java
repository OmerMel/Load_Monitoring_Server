package com.finalproject.load_monitoring.controller;

import com.finalproject.load_monitoring.dto.SensorUpdateDTO;
import com.finalproject.load_monitoring.service.OccupancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "IoT Sensor API", description = "Endpoints for updating carriage occupancy from IoT sensors")
public class SensorController {

    private final OccupancyService occupancyService;

    @PostMapping("/update")
    @Operation(summary = "Update Carriage Occupancy", description = "Update the occupancy of a carriage based on data from IoT sensors.")
    public ResponseEntity<String> updateCarriageOccupancy(@RequestBody SensorUpdateDTO sensorData) {
        occupancyService.updateOccupancy(sensorData);
        return ResponseEntity.ok("Occupancy updated successfully");
    }
}