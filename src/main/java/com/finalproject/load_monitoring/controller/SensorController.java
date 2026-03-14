package com.finalproject.load_monitoring.controller;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.service.OccupancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "IoT Sensor API", description = "Endpoints for updating carriage occupancy from IoT sensors")
public class SensorController {

    private final OccupancyService occupancyService;

    //////////////////////////////////////////////////////////////////////////////////////////////
    /// Update Carriage Occupancy
    @PostMapping("/update")
    @Operation(summary = "Update Carriage Occupancy", description = "Update the occupancy of a carriage based on data from IoT sensors. This endpoint receives data from the Raspberry Pi/MQTT bridge or direct sensor updates.")
    public ResponseEntity<String> updateCarriageOccupancy(@RequestBody SensorDataDTO sensorData) {
        occupancyService.updateOccupancy(sensorData);
        return ResponseEntity.ok("Occupancy updated successfully");
    }
}
