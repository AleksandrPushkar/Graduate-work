package searchengine.exceptions.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.statistics.ConfigSitesNotFoundException;

@ControllerAdvice
public class StatisticsExceptionHandler {

    @ExceptionHandler(ConfigSitesNotFoundException.class)
    protected ResponseEntity<StatisticsResponse> handleConfigSitesNotFoundException() {
        StatisticsResponse response = new StatisticsResponse(false, null,
                "В конфигурационном файле не указаны сайты для индексации");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
