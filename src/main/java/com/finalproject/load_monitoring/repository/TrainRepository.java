package com.finalproject.load_monitoring.repository;

import com.finalproject.load_monitoring.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TrainRepository extends JpaRepository<Train, Long> {

    public List<Train> findAllByDepartureTimeBetween(LocalDateTime start, LocalDateTime end);
    public List<Train> findAllByOriginStationAndDestinationStation(String originStation, String destinationStation);
}
