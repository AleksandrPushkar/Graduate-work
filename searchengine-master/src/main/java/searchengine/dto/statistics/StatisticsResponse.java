package searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class StatisticsResponse {
    private final boolean result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final StatisticsData statistics;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String error;
}
