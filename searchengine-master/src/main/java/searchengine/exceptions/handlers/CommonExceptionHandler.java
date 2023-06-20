package searchengine.exceptions.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.ErrorResponse;
import searchengine.exceptions.common.ConfigSitesNotFoundException;

@ControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(ConfigSitesNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleConfigSitesNotFoundException(ConfigSitesNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(false, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
