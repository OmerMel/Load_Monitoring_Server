package com.finalproject.load_monitoring.converter;

import com.finalproject.load_monitoring.dto.CarriageDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CarriageConverter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Convert From CarriageEntity to CarriageDTO
    public CarriageDTO toDTO(Carriage carriageEntity) {
        if (carriageEntity == null) {
            return null;
        }

        CarriageDTO carriageDto = new CarriageDTO();
        carriageDto.setCarriageId(carriageEntity.getCarriageId());
        
        if (carriageEntity.getTrain() != null) {
            carriageDto.setTrainId(carriageEntity.getTrain().getTrainId());
        }
        
        carriageDto.setCarriageNumber(carriageEntity.getCarriageNumber());
        carriageDto.setOccupancy(carriageEntity.getOccupancy());
        carriageDto.setMaxCapacity(carriageEntity.getMaxCapacity());
        
        if (carriageEntity.getLastUpdated() != null) {
            carriageDto.setLastUpdated(carriageEntity.getLastUpdated().format(FORMATTER));
        }

        return carriageDto;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    /// Convert From CarriageDTO to CarriageEntity
    public Carriage toEntity(CarriageDTO carriageDto) {
        if (carriageDto == null) {
            return null;
        }

        Carriage carriage = new Carriage();
        carriage.setCarriageId(carriageDto.getCarriageId());
        carriage.setCarriageNumber(carriageDto.getCarriageNumber());
        carriage.setOccupancy(carriageDto.getOccupancy());
        carriage.setMaxCapacity(carriageDto.getMaxCapacity());
        
        if (carriageDto.getLastUpdated() != null) {
            carriage.setLastUpdated(LocalDateTime.parse(carriageDto.getLastUpdated(), FORMATTER));
        } else {
            carriage.setLastUpdated(LocalDateTime.now());
        }

        // Note: Train relationship is not set here as it requires fetching the Train entity
        
        return carriage;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
}
