package searchengine.exceptions.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.search.*;

@ControllerAdvice
public class SearchExceptionHandler {

    @ExceptionHandler(EmptySearchQueryException.class)
    protected ResponseEntity<SearchResponse> handleEmptySearchQueryException() {
        SearchResponse response = new SearchResponse(false, null, null,
                "Задан пустой поисковый запрос");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConfigSitesNotFoundException.class)
    protected ResponseEntity<SearchResponse> handleConfigSitesNotFoundException() {
        SearchResponse response = new SearchResponse(false, null, null,
                "Сайты для индексации и посика не добавлены в конфигурационный файл");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntitySiteNotFoundException.class)
    protected ResponseEntity<SearchResponse> handleEntitySiteNotFoundException(
            EntitySiteNotFoundException ex) {

        SearchResponse response = new SearchResponse(false, null, null,
                "Сайт " + ex.getSiteUrl() + " не проиндексирован.");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LemmasFinderNotReadyWorkException.class)
    protected ResponseEntity<SearchResponse> handleLemmasFinderNotReadyWorkException() {
        SearchResponse response = new SearchResponse(false, null, null,
                "Внутренняя ошибка сервера");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NotFoundIndexesException.class)
    protected ResponseEntity<SearchResponse> handleNotFoundIndexesException() {
        SearchResponse response = new SearchResponse(false, null, null,
                "Внутренняя ошибка сервера");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
