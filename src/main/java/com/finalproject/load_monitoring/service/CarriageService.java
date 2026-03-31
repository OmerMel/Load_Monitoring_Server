package com.finalproject.load_monitoring.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.finalproject.load_monitoring.repository.CarriageRepository;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.exception.ResourceNotFoundException;
import com.finalproject.load_monitoring.converter.CarriageConverter;
import com.finalproject.load_monitoring.dto.CarriageDTO;

@Service
@RequiredArgsConstructor
public class CarriageService {
    private final CarriageRepository carriageRepository;
    private final CarriageConverter carriageConverter;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Get carriage details by trainId and carriageId
    public CarriageDTO getCarriageDetailsByTrainIdAndCarriageId(Long trainId, Long carriageId) {
        Carriage carriage = carriageRepository.findByTrainTrainIdAndCarriageId(trainId, carriageId).orElseThrow(() -> new ResourceNotFoundException("Carriage", "number " + carriageId + " in train", trainId));

        return carriageConverter.toDTO(carriage);
    }
}
