package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.SensorUpdateDTO;
import com.finalproject.load_monitoring.exception.ResourceNotFoundException;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.repository.CarriageRepository;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OccupancyService {

    private final CarriageRepository carriageRepository;
    private final OccupancyLogRepository occupancyLogRepository;

    @Transactional
    public void updateOccupancy(SensorUpdateDTO data) {
        // Fetch the carriage based on train ID and carriage number
        Carriage carriage = carriageRepository.findByTrainTrainIdAndCarriageNumber(
                data.getTrainId(),
                data.getCarriageNumber()
        ).orElseThrow(() -> new ResourceNotFoundException("Carriage", "number " + data.getCarriageNumber() + " in train", data.getTrainId()));

        // Update the current occupancy
        carriage.setOccupancy(data.getCameraNumber() + data.getTofNumber());
        carriage.setLastUpdated(LocalDateTime.now());
        carriageRepository.save(carriage);

        // Log the occupancy update
        OccupancyLog log = new OccupancyLog();
        log.setCarriage(carriage);
        log.setCalculatedOccupancy(data.getCameraNumber() + data.getTofNumber());
        log.setTimestamp(LocalDateTime.now());

        // For now, we set cameraCount and sensorCount to 0 as placeholders
        log.setCameraCount(data.getCameraNumber() + data.getTofNumber());
        log.setSensorCount(0);

        occupancyLogRepository.save(log);
    }
}
