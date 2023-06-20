package searchengine.exceptions.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.ErrorResponse;
import searchengine.exceptions.search.*;

@ControllerAdvice
public class SearchExceptionHandler {

    @ExceptionHandler(EmptySearchQueryException.class)
    protected ResponseEntity<ErrorResponse> handleEmptySearchQueryException(EmptySearchQueryException ex) {
        ErrorResponse errorResponse = new ErrorResponse(false, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntitySiteNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleEntitySiteNotFoundException(EntitySiteNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(false, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
