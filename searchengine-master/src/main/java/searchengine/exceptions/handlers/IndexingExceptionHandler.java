package searchengine.exceptions.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.indexing.*;

@ControllerAdvice
public class IndexingExceptionHandler {

    @ExceptionHandler(IndexingAlreadyRunningException.class)
    protected ResponseEntity<IndexingResponse> handleIndexingAlreadyRunningException() {
        IndexingResponse response = new IndexingResponse(
                false, "Индексация уже запущена");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IndexingNotRunningException.class)
    protected ResponseEntity<IndexingResponse> handleIndexingNotRunningException() {
        IndexingResponse response = new IndexingResponse(
                false, "Индексация не запущена");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidUrlException.class)
    protected ResponseEntity<IndexingResponse> handleInvalidUrlException() {
        IndexingResponse response = new IndexingResponse(
                false, "Недопустимый URL-адрес");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SiteNotFoundException.class)
    protected ResponseEntity<IndexingResponse> handleSiteNotFoundException() {
        IndexingResponse response = new IndexingResponse(false, "Данная страница" +
                " находится за пределами сайтов, указанных в конфигурационном файле");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PageCodeNotReceivedException.class)
    protected ResponseEntity<IndexingResponse> handlePageCodeNotReceivedException(
            PageCodeNotReceivedException ex) {

        String errorInfo;
        if (ex.getCode() == 0)
            errorInfo = "Ошибка при получении доступа к странице. Возможная " +
                    "причина: некорректный URL";
        else
            errorInfo = "Ошибка при получении доступа к странице. Код ответа: " + ex.getCode();

        IndexingResponse response = new IndexingResponse(
                false, errorInfo);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LemmasFinderNotReadyWorkException.class)
    protected ResponseEntity<IndexingResponse> handleLemmasFinderNotReadyWorkException() {
        IndexingResponse response = new IndexingResponse(false,
                "Внутренняя ошибка сервера. Индексация страницы не произведена.");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
