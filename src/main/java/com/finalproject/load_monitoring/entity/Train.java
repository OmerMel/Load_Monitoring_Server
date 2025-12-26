package com.finalproject.load_monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trains")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trainId;

    private String originStation;
    private String destinationStation;

    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    private String currentStation;

    @OneToMany(mappedBy = "train")
    @ToString.Exclude
    private List<Carriage> carriages;

    private LocalDateTime lastUpdated;

}
