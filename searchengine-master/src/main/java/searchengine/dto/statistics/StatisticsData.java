package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsData {
    private final TotalStatistics total;
    private final List<DetailedStatisticsItem> detailed;
}
