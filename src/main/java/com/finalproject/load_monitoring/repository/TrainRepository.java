package com.finalproject.load_monitoring.repository;

import com.finalproject.load_monitoring.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TrainRepository extends JpaRepository<Train, Long> {

        ////////////////////////////////////////////////////////////////////////////////////////////
        // Find all trains by origin and destination station names
        public List<Train> findAllByOriginStation_StationNameAndDestinationStation_StationName(String originStation,
                        String destinationStation);

        ////////////////////////////////////////////////////////////////////////////////////////////
        // Find all trains by origin and destination station names and departure time
        public List<Train> findAllByOriginStation_StationNameAndDestinationStation_StationNameAndDepartureTimeAfter(
                        String originStation, String destinationStation, LocalDateTime departureTime);
        ////////////////////////////////////////////////////////////////////////////////////////////
}
