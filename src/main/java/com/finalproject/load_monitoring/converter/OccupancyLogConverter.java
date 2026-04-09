package com.finalproject.load_monitoring.converter;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.dto.OccupancyLogDTO;
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
    // Convert From OccupancyLogEntity to OccupancyLogDTO
    public OccupancyLogDTO toDTO(OccupancyLog entityLog) {
        if (entityLog == null) {
            return null;
        }

        return new OccupancyLogDTO(
            entityLog.getLogId(),
            entityLog.getCarriage() != null ? entityLog.getCarriage().getCarriageId() : null,
            entityLog.getCameraCount(),
            entityLog.getIrCount(),
            entityLog.getCalculatedOccupancy(),
            entityLog.getTimestamp()
        );
    }
}
