package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.converter.StationConverter;
import com.finalproject.load_monitoring.dto.StationDTO;
import com.finalproject.load_monitoring.entity.Station;
import com.finalproject.load_monitoring.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StationService {

    private final StationRepository stationRepository;
    private final StationConverter stationConverter;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get all stations
    public List<StationDTO> getAllStations() {
        List<Station> stations = stationRepository.findAll();
        log.info("Fetched {} stations from the database.", stations.size());
        return stationConverter.toDtoList(stations);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
}
