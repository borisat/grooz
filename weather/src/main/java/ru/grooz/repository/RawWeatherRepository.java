package ru.grooz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grooz.model.RawWeatherData;

public interface RawWeatherRepository extends JpaRepository<RawWeatherData, Long> {
}
