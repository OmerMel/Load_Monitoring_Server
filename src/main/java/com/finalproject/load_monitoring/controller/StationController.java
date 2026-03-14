package com.finalproject.load_monitoring.controller;

import com.finalproject.load_monitoring.dto.StationDTO;
import com.finalproject.load_monitoring.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Station API", description = "Endpoints for retrieving station information")
public class StationController {

    private final StationService stationService;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get All Stations
    @GetMapping
    @Operation(summary = "Get All Stations", description = "Retrieve a list of all available stations.")
    public ResponseEntity<List<StationDTO>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
}
