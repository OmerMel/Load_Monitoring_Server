package com.finalproject.load_monitoring.dto;

import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.Train;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DtoMapper {

    TrainDTO toTrainDTO(Train train);

    List<TrainDTO> toDtoList(List<Train> trains);

    @Mapping(source = "train.trainId", target = "trainId")
    CarriageDTO toCarriageDTO(Carriage carriage);

    Train toEntity(TrainDTO trainDTO);

    @InheritInverseConfiguration
    Carriage toEntity(CarriageDTO carriageDTO);
}
