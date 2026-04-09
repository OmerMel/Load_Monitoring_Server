package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.exception.ResourceNotFoundException;
import com.finalproject.load_monitoring.converter.OccupancyLogConverter;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.repository.CarriageRepository;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import com.finalproject.load_monitoring.dto.OccupancyLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OccupancyService {

    private final CarriageRepository carriageRepository;
    private final OccupancyLogRepository occupancyLogRepository;
    private final OccupancyLogConverter occupancyLogConverter;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Update the occupancy of a carriage
    @Transactional
    public void updateOccupancy(SensorDataDTO data) {
        // Fetch the carriage based on train ID and carriage number
        Carriage carriage = carriageRepository.findByTrainTrainIdAndCarriageNumber(
                data.getTrainId(),
                data.getCarriageNumber()
        ).orElseThrow(() -> new ResourceNotFoundException("Carriage", "number " + data.getCarriageNumber() + " in train", data.getTrainId()));

        // Calculate the occupancy using sensor fusion
        int calculatedOccupancy = processSensorFusion(data);
        data.setCalculatedOccupancy(calculatedOccupancy);

        // Update carriage with the current occupancy
        carriage.setOccupancy(data.getCalculatedOccupancy());
        carriage.setLastUpdated(LocalDateTime.now());
        carriageRepository.save(carriage);

        // Create a new occupancy log
        OccupancyLog occupancyLog = occupancyLogConverter.toEntity(data, carriage);

        occupancyLogRepository.save(occupancyLog);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Process the sensor fusion
    public int processSensorFusion(SensorDataDTO data) {
        log.info("Processing sensor fusion for Train {}, Carriage {}", data.getTrainId(), data.getCarriageNumber());
        
        // Simple fusion logic: sum of camera and IR counts
        // TODO: Implement the actual sensor fusion logic
        return data.getCameraCount() + data.getIrCount();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get the sensors data (camera counter and IR counter)
    @Transactional(readOnly = true)
    public Optional<OccupancyLogDTO> getSensorsData(Long carriageId) {
        return occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId)
                .map(occupancyLogConverter::toDTO);
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
}
