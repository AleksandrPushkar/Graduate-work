package searchengine.dto.indexing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class IndexingResponse {
    private final boolean result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String error;
}
