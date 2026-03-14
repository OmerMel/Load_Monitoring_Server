package com.finalproject.load_monitoring.converter;

import com.finalproject.load_monitoring.dto.StationDTO;
import com.finalproject.load_monitoring.entity.Station;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StationConverter {

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Convert From StationEntity to StationDTO
    public StationDTO toDTO(Station stationEntity) {
        if (stationEntity == null) {
            return null;
        }
        return new StationDTO(stationEntity.getStationId(), stationEntity.getStationName());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Convert From StationDTO to StationEntity
    public Station toEntity(StationDTO stationDto) {
        if (stationDto == null) {
            return null;
        }
        return new Station(stationDto.getStationId(), stationDto.getStationName());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Convert From List<StationEntity> to List<StationDTO>
    public List<StationDTO> toDtoList(List<Station> stationEntities) {
        if (stationEntities == null) {
            return new ArrayList<>();
        }
        return stationEntities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
}
