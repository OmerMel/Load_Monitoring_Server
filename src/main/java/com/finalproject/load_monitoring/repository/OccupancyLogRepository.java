package com.finalproject.load_monitoring.repository;

import com.finalproject.load_monitoring.entity.OccupancyLog;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccupancyLogRepository extends JpaRepository<OccupancyLog,Long> {
    
    // Find the latest log by carriageId
    Optional<OccupancyLog> findFirstByCarriage_CarriageIdOrderByTimestampDesc(Long carriageId);

    // Find the 3 latest logs by carriageId
    List<OccupancyLog> findTop3ByCarriage_CarriageIdOrderByTimestampDesc(Long carriageId);
}
