package com.finalproject.load_monitoring.controller;

import com.finalproject.load_monitoring.dto.TrainDTO;
import com.finalproject.load_monitoring.service.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
@Tag(name = "Passenger API", description = "Endpoints for the passenger app and digital signage")
public class PassengerController {

    private final TrainService trainService;

    @GetMapping
    @Operation(summary = "Get All Trains", description = "Retrieve a list of all trains with their basic information.")
    public ResponseEntity<List<TrainDTO>> getAllTrains() {
        return ResponseEntity.ok(trainService.getAllTrains());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Train Details", description = "Retrieve detailed information about a specific train by its ID.")
    public ResponseEntity<TrainDTO> getTrainById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(trainService.getTrainDetails(id));
    }

    @GetMapping("/{origin}/{destination}")
    public ResponseEntity<List<TrainDTO>> getAllTrainsByOriginAndDestination(@PathVariable String origin, @PathVariable String destination) {
        return ResponseEntity.ok(trainService.getAllTrainsByOriginAndDestination(origin, destination));
    }
}
