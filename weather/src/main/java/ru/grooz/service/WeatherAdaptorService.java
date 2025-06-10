package ru.grooz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grooz.dto.SourceWeatherData;
import ru.grooz.dto.WeatherData;
import ru.grooz.model.NormalizedWeatherData;
import ru.grooz.model.RawWeatherData;
import ru.grooz.repository.NormalizedWeatherRepository;
import ru.grooz.repository.RawWeatherRepository;
import ru.grooz.util.WeatherDataParser;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherAdaptorService {

    private final RawWeatherRepository rawWeatherRepository;
    private final NormalizedWeatherRepository normalizedWeatherRepository;
    private final WeatherDataParser weatherDataParser;

    public void processAndSaveWeatherData(SourceWeatherData sourceData) {
        log.info("Starting to process data for sourceId: {}", sourceData.getSourceId());

        RawWeatherData savedRawData = saveRawData(sourceData);
        WeatherData weatherData = parseData(sourceData, savedRawData);
        NormalizedWeatherData normalizedData = createNormalizedData(sourceData, weatherData, savedRawData);
        saveNormalizedData(normalizedData, sourceData);
    }

    private RawWeatherData saveRawData(SourceWeatherData sourceData) {
        RawWeatherData rawData = new RawWeatherData();
        rawData.setSourceId(sourceData.getSourceId());
        rawData.setPayload(sourceData.getPayload());
        rawData.setTimestamp(LocalDateTime.now());

        try {
            return rawWeatherRepository.save(rawData);
        } catch (Exception e) {
            log.error("Failed to save raw data for sourceId {}: {}", sourceData.getSourceId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save raw data for source " + sourceData.getSourceId(), e);
        }
    }

    private WeatherData parseData(SourceWeatherData sourceData, RawWeatherData savedRawData) {
        try {
            return weatherDataParser.parse(sourceData.getPayload());
        } catch (Exception e) {
            log.error("Failed to parse weather data for sourceId {}. Payload: '{}'. Error: {}",
                    sourceData.getSourceId(), sourceData.getPayload(), e.getMessage(), e);
            throw new RuntimeException("Parsing failed for source " + sourceData.getSourceId(), e);
        }
    }

    private NormalizedWeatherData createNormalizedData(
            SourceWeatherData sourceData,
            WeatherData weatherData,
            RawWeatherData savedRawData) {
        NormalizedWeatherData normalizedData = new NormalizedWeatherData();
        normalizedData.setSourceId(sourceData.getSourceId());
        normalizedData.setTemperature(Optional.ofNullable(weatherData).map(WeatherData::getTemperature).orElse(0.0));
        normalizedData.setHumidity(Optional.ofNullable(weatherData).map(WeatherData::getHumidity).orElse(0.0));
        normalizedData.setTimestamp(LocalDateTime.now());
        normalizedData.setRawData(savedRawData);
        return normalizedData;
    }

    private void saveNormalizedData(NormalizedWeatherData normalizedData, SourceWeatherData sourceData) {
        try {
            normalizedWeatherRepository.save(normalizedData);
            log.info("Normalized data saved for sourceId: {} with linked rawData id: {}",
                    sourceData.getSourceId(), normalizedData.getRawData().getId());
        } catch (Exception e) {
            log.error("Failed to save normalized data for sourceId {}: {}", sourceData.getSourceId(), e.getMessage(), e);
            throw new RuntimeException("Saving normalized data failed for source " + sourceData.getSourceId(), e);
        }
    }
}