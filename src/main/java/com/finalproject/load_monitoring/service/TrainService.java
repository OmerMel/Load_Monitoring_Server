package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.converter.TrainConverter;
import com.finalproject.load_monitoring.dto.TrainDTO;
import com.finalproject.load_monitoring.entity.Train;
import com.finalproject.load_monitoring.exception.ResourceNotFoundException;
import com.finalproject.load_monitoring.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainService {

    public final TrainRepository trainRepository;
    public final TrainConverter trainConverter;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get all trains
    public List<TrainDTO> getAllTrains() {
        List<Train> trains = trainRepository.findAll();
        return trainConverter.toDtoList(trains);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get train details
    public TrainDTO getTrainDetails(Long trainId) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + trainId));

        return trainConverter.toDTO(train);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get all trains by origin and destination
    public List<TrainDTO> getAllTrainsByOriginAndDestination(String origin, String destination) {
        List<Train> trains = trainRepository.findAllByOriginStation_StationNameAndDestinationStation_StationName(origin,
                destination);
        return trainConverter.toDtoList(trains);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get all trains by origin and destination and departure time
    public List<TrainDTO> getAllTrainsByOriginAndDestinationAndDepartureTime(String origin, String destination,
            LocalDateTime departureTime) {
        List<Train> trains = trainRepository
                .findAllByOriginStation_StationNameAndDestinationStation_StationNameAndDepartureTimeAfter(origin,
                        destination, departureTime);
        return trainConverter.toDtoList(trains);
    }
}
