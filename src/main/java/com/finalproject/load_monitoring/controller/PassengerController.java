package com.finalproject.load_monitoring.controller;

import com.finalproject.load_monitoring.dto.TrainDTO;
import com.finalproject.load_monitoring.service.CarriageService;
import com.finalproject.load_monitoring.service.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;
import com.finalproject.load_monitoring.dto.CarriageDTO;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
@Tag(name = "Passenger API", description = "Endpoints for the passenger app and digital signage")
public class PassengerController {

    private final TrainService trainService;
    private final CarriageService carriageService;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get All Trains
    @GetMapping
    @Operation(summary = "Get All Trains", description = "Retrieve a list of all trains with their basic information.")
    public ResponseEntity<List<TrainDTO>> getAllTrains() {
        return ResponseEntity.ok(trainService.getAllTrains());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get Train Details
    @GetMapping("/{id}")
    @Operation(summary = "Get Train Details", description = "Retrieve detailed information about a specific train by its ID.")
    public ResponseEntity<TrainDTO> getTrainById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(trainService.getTrainDetails(id));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Search Trains By Origin and Destination
    @GetMapping("/search/{origin}/{destination}")
    @Operation(summary = "Search Trains", description = "Search for trains traveling between a specific origin and destination.")
    public ResponseEntity<List<TrainDTO>> getAllTrainsByOriginAndDestination(@PathVariable String origin,
            @PathVariable String destination) {
        return ResponseEntity.ok(trainService.getAllTrainsByOriginAndDestination(origin, destination));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get Trains By Origin and Destination and Departure Time
    @GetMapping("/search/{origin}/{destination}/{departureTime}")
    @Operation(summary = "Search Trains by Time", description = "Search for trains traveling between a specific origin and destination and departing from a specific departure time.")
    public ResponseEntity<List<TrainDTO>> getAllTrainsByOriginAndDestinationAndDepartureTime(
            @PathVariable String origin, @PathVariable String destination, @PathVariable String departureTime) {
        LocalDateTime parsedDepartureTime = LocalDateTime.parse(departureTime);
        return ResponseEntity.ok(trainService.getAllTrainsByOriginAndDestinationAndDepartureTime(origin, destination,
                parsedDepartureTime));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get carriage details by carriage by trainId and carriageId
    @GetMapping("/carriage/{trainId}/{carriageId}")
    @Operation(summary = "Get Carriage Details", description = "Retrieve detailed information about a specific carriage by its train ID and carriage ID.")
    public ResponseEntity<CarriageDTO> getCarriageDetailsByTrainIdAndCarriageId(@PathVariable Long trainId, @PathVariable Long carriageId) {
        return ResponseEntity.ok(carriageService.getCarriageDetailsByTrainIdAndCarriageId(trainId, carriageId));
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
}
