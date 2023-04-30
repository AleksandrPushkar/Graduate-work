package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.EntitySite;
import searchengine.services.indexing.IndexingService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final IndexingService indexingService;

    @Override
    public StatisticsResponse getStatistics() {
        if(sites.getSites().size() == 0)
            return new StatisticsResponse(false, null,
                    InfoErrorStatistics.getErrorInfoWhenGettingStatistics());

        StatisticsData data = createStatisticsData();
        return new StatisticsResponse(true, data, null);
    }

    @Override
    public StatisticsData createStatisticsData() {
        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        total.setSites(sitesList.size());
        total.setIndexing(indexingService.isIndexingRunning());
        for (Site siteConfig : sitesList) {
            DetailedStatisticsItem item
                    = createDetailedStatisticsItem(siteConfig);
            detailed.add(item);
            total.setPages(total.getPages() + item.getPages());
            total.setLemmas(total.getLemmas() + item.getLemmas());
        }

        return new StatisticsData(total, detailed);
    }

    @Override
    public DetailedStatisticsItem createDetailedStatisticsItem(Site siteConfig) {
        EntitySite site = indexingService
                .findSiteByUrl(siteConfig.getUrl());

        DetailedStatisticsItem item = new DetailedStatisticsItem(
                siteConfig.getUrl(), siteConfig.getName());
        if (site != null) {
            item.setStatus(site.getStatus().toString());
            if (site.getLastError() != null)
                item.setError(site.getLastError());

            item.setPages(indexingService.getCountPagesInSite(site));
            item.setLemmas(indexingService.getCountLemmasInSite(site));
            ZonedDateTime zoneDateTime = site
                    .getStatusTime().atZone(ZoneId.of("Europe/Moscow"));
            long time = zoneDateTime.toInstant().toEpochMilli();
            item.setStatusTime(time);
        }

        return item;
    }
}
