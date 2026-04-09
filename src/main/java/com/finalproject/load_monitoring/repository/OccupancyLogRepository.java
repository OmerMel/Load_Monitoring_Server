package com.finalproject.load_monitoring.repository;

import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.dto.SensorDataDTO;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccupancyLogRepository extends JpaRepository<OccupancyLog,Long> {
    
    // Find the latest log by carriageId
    Optional<OccupancyLog> findFirstByCarriage_CarriageIdOrderByTimestampDesc(Long carriageId);
}
