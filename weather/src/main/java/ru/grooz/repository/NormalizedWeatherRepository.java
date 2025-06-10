package ru.grooz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.grooz.model.NormalizedWeatherData;

public interface NormalizedWeatherRepository extends JpaRepository<NormalizedWeatherData, Long> {
}
