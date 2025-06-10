package ru.grooz.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_weather_data")
@Data
public class RawWeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private int sourceId;
    @Column(columnDefinition = "TEXT")
    private String payload;
    private LocalDateTime timestamp;
}