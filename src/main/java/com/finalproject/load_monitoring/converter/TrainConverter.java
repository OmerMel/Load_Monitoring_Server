package com.finalproject.load_monitoring.converter;

import com.finalproject.load_monitoring.dto.CarriageDTO;
import com.finalproject.load_monitoring.dto.TrainDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.Train;
import com.finalproject.load_monitoring.repository.StationRepository;
import com.finalproject.load_monitoring.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TrainConverter {

    private final CarriageConverter carriageConverter;
    private final StationRepository stationRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Convert From TrainEntity to TrainDTO
    public TrainDTO toDTO(Train trainEntity) {
        if (trainEntity == null) {
            return null;
        }

        TrainDTO trainDto = new TrainDTO();
        trainDto.setTrainId(trainEntity.getTrainId());
        
        if (trainEntity.getOriginStation() != null) {
            trainDto.setOriginStation(trainEntity.getOriginStation().getStationName());
        }
        
        if (trainEntity.getDestinationStation() != null) {
            trainDto.setDestinationStation(trainEntity.getDestinationStation().getStationName());
        }
        
        if (trainEntity.getDepartureTime() != null) {
            trainDto.setDepartureTime(trainEntity.getDepartureTime().format(FORMATTER));
        }
        
        if (trainEntity.getArrivalTime() != null) {
            trainDto.setArrivalTime(trainEntity.getArrivalTime().format(FORMATTER));
        }
        
        if (trainEntity.getLastUpdated() != null) {
            trainDto.setLastUpdated(trainEntity.getLastUpdated().format(FORMATTER));
        }

        if (trainEntity.getCarriages() != null) {
            List<CarriageDTO> carriageDtos = trainEntity.getCarriages().stream()
                    .sorted(Comparator.comparingInt(Carriage::getCarriageNumber))
                    .map(carriageConverter::toDTO)
                    .collect(Collectors.toList());
                    trainDto.setCarriages(carriageDtos);
        } else {
            trainDto.setCarriages(new ArrayList<>());
        }

        return trainDto;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Convert From List<TrainEntity> to List<TrainDTO>
    public List<TrainDTO> toDtoList(List<Train> trainEntities) {
        if (trainEntities == null) {
            return new ArrayList<>();
        }
        return trainEntities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Convert From TrainDTO to TrainEntity
    public Train toEntity(TrainDTO trainDto) {
        if (trainDto == null) {
            return null;
        }

        Train train = new Train();
        train.setTrainId(trainDto.getTrainId());
        
        // Handle Station relationships
        if (trainDto.getOriginStation() != null) {
            train.setOriginStation(stationRepository.findByStationName(trainDto.getOriginStation())
                    .orElseThrow(() -> new ResourceNotFoundException("Station", "name", trainDto.getOriginStation())));
        }

        if (trainDto.getDestinationStation() != null) {
            train.setDestinationStation(stationRepository.findByStationName(trainDto.getDestinationStation())
                    .orElseThrow(() -> new ResourceNotFoundException("Station", "name", trainDto.getDestinationStation())));
        }

        // Handle Carriages relationship
        if (trainDto.getCarriages() != null) {
            List<Carriage> carriages = trainDto.getCarriages().stream()
                    .map(carriageConverter::toEntity)
                    .collect(Collectors.toList());
            
            // Set the parent reference for each carriage
            carriages.forEach(carriage -> carriage.setTrain(train));
            train.setCarriages(carriages);
        } else {
            train.setCarriages(new ArrayList<>());
        }
        
        if (trainDto.getDepartureTime() != null) {
            train.setDepartureTime(LocalDateTime.parse(trainDto.getDepartureTime(), FORMATTER));
        }
        
        if (trainDto.getArrivalTime() != null) {
            train.setArrivalTime(LocalDateTime.parse(trainDto.getArrivalTime(), FORMATTER));
        }
        
        if (trainDto.getLastUpdated() != null) {
            train.setLastUpdated(LocalDateTime.parse(trainDto.getLastUpdated(), FORMATTER));
        } else {
            train.setLastUpdated(LocalDateTime.now());
        }

        return train;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
}
