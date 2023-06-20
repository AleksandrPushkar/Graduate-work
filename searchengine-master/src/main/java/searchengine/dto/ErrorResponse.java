package searchengine.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private final boolean result;
    private final String error;
}
