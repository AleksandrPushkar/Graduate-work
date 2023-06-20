package searchengine.exceptions.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.ErrorResponse;
import searchengine.exceptions.indexing.*;

@ControllerAdvice
public class IndexingExceptionHandler {

    @ExceptionHandler(IndexingAlreadyRunningException.class)
    protected ResponseEntity<ErrorResponse> handleIndexingAlreadyRunningException(IndexingAlreadyRunningException ex) {
        ErrorResponse errorResponse = new ErrorResponse(false, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IndexingNotRunningException.class)
    protected ResponseEntity<ErrorResponse> handleIndexingNotRunningException(IndexingNotRunningException ex) {
        ErrorResponse errorResponse = new ErrorResponse(false, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidUrlException.class)
    protected ResponseEntity<ErrorResponse> handleInvalidUrlException(InvalidUrlException ex) {
        ErrorResponse errorResponse = new ErrorResponse(false, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SiteNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleSiteNotFoundException(SiteNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(false, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PageCodeNotReceivedException.class)
    protected ResponseEntity<ErrorResponse> handlePageCodeNotReceivedException(PageCodeNotReceivedException ex) {
        ErrorResponse errorResponse = new ErrorResponse(false, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
