package com.finalproject.load_monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "carriages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Carriage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long carriageId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id")
    @ToString.Exclude
    private Train train;
    private int carriageNumber;
    private int occupancy;
    private int maxCapacity;
    private LocalDateTime lastUpdated;
}
