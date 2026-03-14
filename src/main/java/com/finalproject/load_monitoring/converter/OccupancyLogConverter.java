package com.finalproject.load_monitoring.converter;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OccupancyLogConverter {

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Convert From SensorDataDTO and CarriageEntity to OccupancyLogEntity
    public OccupancyLog toEntity(SensorDataDTO data, Carriage carriage) {
        if (data == null) {
            return null;
        }

        OccupancyLog log = new OccupancyLog();
        log.setCarriage(carriage);
        log.setCameraCount(data.getCameraCount());
        log.setIrCount(data.getIrCount());
        log.setCalculatedOccupancy(data.getCalculatedOccupancy());
        log.setTimestamp(LocalDateTime.now());
        
        return log;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
}
