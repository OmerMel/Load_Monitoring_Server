package com.finalproject.load_monitoring.repository;

import com.finalproject.load_monitoring.entity.Carriage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarriageRepository extends JpaRepository<Carriage, Long> {

    public Optional<Carriage> findByTrainTrainIdAndCarriageNumber(Long trainId, int carriageNumber);
    public Optional<Carriage> findByTrainTrainIdAndCarriageId(Long trainId, Long carriageId);
    public List<Carriage> findByTrainTrainIdOrderByCarriageNumberAsc(Long trainId);
}
