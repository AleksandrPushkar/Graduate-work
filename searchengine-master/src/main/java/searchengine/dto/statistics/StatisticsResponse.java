package searchengine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsResponse {
    private final boolean result;
    private final StatisticsData statistics;
}
