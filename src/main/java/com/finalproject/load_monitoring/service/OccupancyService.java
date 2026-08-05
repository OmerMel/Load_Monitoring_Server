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
    private final KalmanSensorFusionService sensorFusionService;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Update the occupancy of a carriage
    @Transactional
    public void updateOccupancy(SensorDataDTO data) {
        // Fetch the carriage based on train ID and carriage number
        Carriage carriage = carriageRepository.findByTrainTrainIdAndCarriageNumber(
                data.getTrainId(),
                data.getCarriageNumber()
        ).orElseThrow(() -> new ResourceNotFoundException("Carriage", "number " + data.getCarriageNumber() + " in train", data.getTrainId()));

        boolean cameraOk = !"unavailable".equals(data.getCameraStatus());
        boolean irOk = !"unavailable".equals(data.getIrStatus());

        int calculatedOccupancy;
        if (cameraOk && irOk) {
            // Both sources available - run the normal Kalman sensor fusion
            calculatedOccupancy = sensorFusionService.calculateOccupancy(data, carriage.getCarriageId());
        } else if (cameraOk) {
            // IR is down - trust the camera reading directly, skip fusion entirely
            log.warn("Carriage {} | IR sensor unavailable, using camera count directly: {}",
                    carriage.getCarriageId(), data.getCameraCount());
            calculatedOccupancy = Math.max(0, data.getCameraCount());
            data.setCalculatedUncertainty(null); // forces Kalman to re-initialize once both sources are back
        } else if (irOk) {
            // Camera is down - trust the IR count directly (it's already an absolute headcount)
            log.warn("Carriage {} | Camera unavailable, using IR count directly: {}",
                    carriage.getCarriageId(), data.getIrCount());
            calculatedOccupancy = Math.max(0, data.getIrCount());
            data.setCalculatedUncertainty(null);
        } else {
            // Both sources are down - keep the last known occupancy instead of guessing
            log.warn("Carriage {} | Both camera and IR unavailable, keeping last known occupancy: {}",
                    carriage.getCarriageId(), carriage.getOccupancy());
            calculatedOccupancy = carriage.getOccupancy();
            data.setCalculatedUncertainty(null);
        }
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
    // Get the sensors data (camera counter and IR counter)
    @Transactional(readOnly = true)
    public Optional<OccupancyLogDTO> getSensorsData(Long carriageId) {
        return occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId)
                .map(occupancyLogConverter::toDTO);
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
}
