package ru.grooz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import ru.grooz.dto.SourceWeatherData;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherCollectorService {

    private final WebClient webClient;
    private final WeatherAdaptorService weatherAdaptorService;

    @Value("${weather.sources.base-url}")
    private String baseUrl;
    @Value("${weather.sources.count}")
    private int sourceCount;
    @Value("${weather.sources.retry.max-attempts}")
    private long retryMaxAttempts;
    @Value("${weather.sources.retry.delay-seconds}")
    private long retryDelaySeconds;

    /**
     * Собирает и обрабатывает данные о погоде со всех сконфигурированных источников.
     * Запускает асинхронные запросы, обрабатывает ошибки и сохраняет данные.
     *
     * @return Mono<Void>, сигнализирующий о завершении процесса.
     */
    public Mono<Void> collectAndProcessAllWeatherData() {
        List<Mono<SourceWeatherData>> sourceRequests = IntStream.range(1, sourceCount + 1)
                .mapToObj(this::buildSourceRequest)
                .toList();

        return Flux.merge(sourceRequests)
                .flatMap(this::processSourceResponse)
                .then();
    }

    /**
     * Создает Mono<String> для запроса данных от конкретного источника.
     * Включает логику ретрая и обработки ошибок на уровне сетевого запроса.
     *
     * @param sourceId Идентификатор источника данных.
     * @return Mono<String> с payload данных от источника, или Mono.empty() в случае неисправимой ошибки.
     */
    private Mono<SourceWeatherData> buildSourceRequest(int sourceId) {
        return fetchWeatherData(sourceId)
                .retryWhen(Retry.fixedDelay(retryMaxAttempts, Duration.ofSeconds(retryDelaySeconds))
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying source {} due to error: {}",
                                        sourceId,
                                        retrySignal.failure().getMessage()))
                        // Исключаем повторные попытки для ошибок парсинга (если IllegalArgumentException)
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
                .onErrorResume(e -> {
                    log.error("Failed to fetch data from source {}: {}", sourceId, e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Выполняет HTTP-запрос к указанному источнику данных.
     *
     * @param sourceId Идентификатор источника.
     * @return Mono<String> содержащий ID источника и его payload (например, "1::json_payload").
     */
    private Mono<SourceWeatherData> fetchWeatherData(int sourceId) {
        return webClient.get()
                .uri(baseUrl + sourceId)
                .retrieve()
                .bodyToMono(String.class)
                .map(payload -> new SourceWeatherData(sourceId, payload));
    }

    /**
     * Обрабатывает ответ от источника данных: парсит id и payload,
     * вызывает адаптер для сохранения данных.
     * Эта операция выполняется на отдельном пуле потоков, чтобы не блокировать реактивный евент луп.
     *
     * @param sourceData Объект, содержащий id источника и payload, разделенные "::".
     * @return Mono<Void>, сигнализирующий о завершении обработки данного ответа.
     */
    private Mono<Void> processSourceResponse(SourceWeatherData sourceData) {
        int sourceId = sourceData.getSourceId();

        return Mono.fromRunnable(() -> {
                    try {
                        weatherAdaptorService.processAndSaveWeatherData(sourceData);
                    } catch (Exception e) {
                        log.error("Failed to process and save data for source {}: {}", sourceId, e.getMessage());
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Error during processing and saving data for source {}: {}", sourceId, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}