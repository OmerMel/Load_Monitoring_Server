package com.finalproject.load_monitoring.repository;

import com.finalproject.load_monitoring.entity.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccupancyLogRepository extends JpaRepository<OccupancyLog,Long> {
}
