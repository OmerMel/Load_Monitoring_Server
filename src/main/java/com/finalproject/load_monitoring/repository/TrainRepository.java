package com.finalproject.load_monitoring.repository;

import com.finalproject.load_monitoring.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainRepository extends JpaRepository<Train, Long> {
}
