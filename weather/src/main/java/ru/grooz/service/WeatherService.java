package ru.grooz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.grooz.dto.AggregatedWeatherResponse;
import ru.grooz.model.NormalizedWeatherData;
import ru.grooz.repository.NormalizedWeatherRepository;

import java.util.List;
import java.util.Locale;


@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherCollectorService weatherCollectorService;
    private final NormalizedWeatherRepository normalizedWeatherRepository;

    /**
     * Агрегирует данные о погоде, выполняя следующие шаги:
     * 1. Запускает сбор и обработку новых данных от внешних источников.
     * 2. После успешного завершения сбора, извлекает все нормализованные данные из базы данных.
     * 3. Рассчитывает среднюю температуру и влажность на основе полученных данных.
     *
     * @return Mono<AggregatedWeatherResponse> содержащий среднюю температуру и влажность,
     * или дефолтные значения (0.0, 0.0), если данных нет.
     */
    public Mono<AggregatedWeatherResponse> getAggregatedWeatherData() {
        return weatherCollectorService.collectAndProcessAllWeatherData()
                .then(Mono.defer(this::fetchAllNormalizedData)
                        .map(this::calculateAggregatedResponse)
                );
    }

    /**
     * Извлекает все нормализованные данные о погоде из репозитория.
     * Эта операция блокирующая (Spring Data JPA), поэтому выполняется на отдельном пуле потоков
     * (Schedulers.boundedElastic), чтобы не блокировать основной Event Loop WebFlux.
     *
     * @return Mono<List < NormalizedWeatherData>> список всех нормализованных данных.
     */
    private Mono<List<NormalizedWeatherData>> fetchAllNormalizedData() {
        return Mono.fromCallable(normalizedWeatherRepository::findAll)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Рассчитывает агрегированные средние значения температуры и влажности
     * на основе списка нормализованных данных.
     * Результаты округляются до двух знаков после запятой.
     *
     * @param allNormalizedData Список нормализованных данных о погоде.
     * @return AggregatedWeatherResponse с рассчитанными средними значениями.
     * Возвращает дефолтный ответ (0.0, 0.0), если список пуст.
     */
    private AggregatedWeatherResponse calculateAggregatedResponse(List<NormalizedWeatherData> allNormalizedData) {
        if (allNormalizedData.isEmpty()) {
            return new AggregatedWeatherResponse(0.0, 0.0);
        }

        double totalTemperature = allNormalizedData.stream()
                .mapToDouble(NormalizedWeatherData::getTemperature)
                .sum();
        double totalHumidity = allNormalizedData.stream()
                .mapToDouble(NormalizedWeatherData::getHumidity)
                .sum();

        double averageTemperature = totalTemperature / allNormalizedData.size();
        double averageHumidity = totalHumidity / allNormalizedData.size();

        String formattedAvgTemp = String.format(Locale.ENGLISH, "%.2f", averageTemperature);
        String formattedAvgHum = String.format(Locale.ENGLISH, "%.2f", averageHumidity);

        double roundedAverageTemperature = Double.parseDouble(formattedAvgTemp);
        double roundedAverageHumidity = Double.parseDouble(formattedAvgHum);

        return new AggregatedWeatherResponse(roundedAverageTemperature, roundedAverageHumidity);
    }
}