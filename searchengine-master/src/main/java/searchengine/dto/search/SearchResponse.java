package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private final boolean result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Integer count;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<PageItem> data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String error;
}
