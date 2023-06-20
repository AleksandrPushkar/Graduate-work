package searchengine.services.statistics;

import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;

@Service
public interface StatisticsService {
    StatisticsResponse getStatistics();

    StatisticsData createStatisticsData();

    DetailedStatisticsItem createDetailedStatisticsItem(Site siteConfig);
}
