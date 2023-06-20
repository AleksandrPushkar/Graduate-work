package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.exceptions.common.ConfigSitesNotFoundException;
import searchengine.model.EntitySite;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sitesListObj;
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private final LemmaRepository lemmaRepo;
    private final IndexingService indexingService;

    @Override
    public StatisticsResponse getStatistics() {
        if(sitesListObj.getSites().isEmpty()) {
            throw new ConfigSitesNotFoundException();
        }
        StatisticsData data = createStatisticsData();
        return new StatisticsResponse(true, data);
    }

    @Override
    public StatisticsData createStatisticsData() {
        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sitesListObj.getSites();
        total.setSites(sitesList.size());
        total.setIndexing(indexingService.isIndexingRunning());
        for (Site siteConfig : sitesList) {
            DetailedStatisticsItem item = createDetailedStatisticsItem(siteConfig);
            detailed.add(item);
            total.setPages(total.getPages() + item.getPages());
            total.setLemmas(total.getLemmas() + item.getLemmas());
        }
        return new StatisticsData(total, detailed);
    }

    @Override
    public DetailedStatisticsItem createDetailedStatisticsItem(Site siteConfig) {
        DetailedStatisticsItem item = new DetailedStatisticsItem(siteConfig.getUrl(), siteConfig.getName());
        EntitySite site = siteRepo.findByUrl(siteConfig.getUrl());
        if (site != null) {
            item.setStatus(site.getStatus().toString());
            if (site.getLastError() != null) {
                item.setError(site.getLastError());
            }
            item.setPages(pageRepo.countAllBySite(site));
            item.setLemmas(lemmaRepo.countAllBySite(site));
            ZonedDateTime zoneDateTime = site.getStatusTime().atZone(ZoneId.of("Europe/Moscow"));
            long time = zoneDateTime.toInstant().toEpochMilli();
            item.setStatusTime(time);
        }
        return item;
    }
}
