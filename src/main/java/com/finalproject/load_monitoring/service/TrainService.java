package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.DtoMapper;
import com.finalproject.load_monitoring.dto.TrainDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.Train;
import com.finalproject.load_monitoring.exception.ResourceNotFoundException;
import com.finalproject.load_monitoring.repository.CarriageRepository;
import com.finalproject.load_monitoring.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainService {

    public final TrainRepository trainRepository;
    private final CarriageRepository carriageRepository;
    public final DtoMapper dtoMapper;

    /**
     * Retrieves all trains from the repository and maps them to DTOs.
     *
     * @return A list of TrainDTO objects representing all trains.
     */
    public List<TrainDTO> getAllTrains() {
        List<Train> trains = trainRepository.findAll();
        return dtoMapper.toDtoList(trains);
    }


    /**
     * Retrieves detailed information about a specific train by its ID.
     *
     * @param trainId The ID of the train to retrieve.
     * @return A TrainDTO object containing detailed information about the train.
     * @throws RuntimeException if the train with the specified ID is not found.
     */
    public TrainDTO getTrainDetails(Long trainId) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + trainId));
        //TODO: throw custom exception

        List<Carriage> sortedCarriages = carriageRepository.findByTrainTrainIdOrderByCarriageNumberAsc(trainId);

        return dtoMapper.toTrainDTO(train);
    }

    public List<TrainDTO> getAllTrainsByOriginAndDestination(String origin, String destination) {
        List<Train> trains = trainRepository.findAllByOriginStationAndDestinationStation(origin, destination);
        return dtoMapper.toDtoList(trains);
    }
}
