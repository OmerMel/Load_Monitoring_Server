package com.finalproject.load_monitoring.repository;

import com.finalproject.load_monitoring.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByStationName(String stationName);
}
