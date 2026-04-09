package com.finalproject.load_monitoring.controller;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.service.OccupancyService;
import com.finalproject.load_monitoring.dto.OccupancyLogDTO;
import java.util.Optional;
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
    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get sensors data (camera counter and IR counter)
    @GetMapping("/{carriageId}")
    @Operation(summary = "Get Camera Counter and IR Counter", description = "Retrieve the camera counter and IR counter for a specific carriage.")
    public ResponseEntity<Optional<OccupancyLogDTO>> getSensorsData(@PathVariable Long carriageId) {
        return ResponseEntity.ok(occupancyService.getSensorsData(carriageId));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
}

