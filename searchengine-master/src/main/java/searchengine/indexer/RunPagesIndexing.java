package searchengine.indexer;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingService;

import java.net.URL;
import java.util.concurrent.ForkJoinPool;

@Component
@RequiredArgsConstructor
public class RunPagesIndexing {
    private final SitesList sitesListObj;
    private final JsoupConnect jsoupCon;
    private final LemmaFinder lemmaFinder;
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private final WorkingWithUrl workerUrl;
    private final WorkingWithDatabase workerDB;
    private final IndexingService indexingService;
    private static Logger logger = LogManager.getLogger(RunPagesIndexing.class);

    private final String infoErrorInvalidUrl = "Некорректный URL или не соответствует шаблону \"URL для индексации\"";
    private final String infoErrorIndexingStopped = "Индексация остановлена пользователем";

    public void indexing(Site configSite) {
        logger.info("Эта запись будет залогирована");
        workerDB.deleteSite(configSite);
        EntitySite site = workerDB.saveNewSite(configSite);
        URL urlObj = checkUrlSite(site);
        if (urlObj == null) {
            return;
        }
        EntityPage page = new EntityPage(site, urlObj.getPath());
        Document doc = jsoupCon.getPageCode(urlObj.toString(), page, site);
        if (doc == null) {
            pageCodeNotReceivedStopIndexing(page, site);
            return;
        }
        pageRepo.save(page);
        workerDB.updateSiteStatusTime(site);
        workerDB.loopSaveLemmasAndIndexes(doc, page, site);
        if (!indexingService.isIndexingStopped()) {
            runForkJoinPool(urlObj, doc, site);
        }
        forkJoinPoolEndedStopIndexing(site);
    }

    private URL checkUrlSite(EntitySite site) {
        String url = workerUrl.urlCorrection(site.getUrl());
        URL urlObj = workerUrl.getURL(url);
        if (urlObj == null || workerUrl.checkForDisqualification(url)) {
            urlFailsValidationStopIndexing(site);
            return null;
        }
        return urlObj;
    }

    private void urlFailsValidationStopIndexing(EntitySite site) {
        site.setStatus(StatusIndexing.FAILED);
        site.setLastError(infoErrorInvalidUrl);
        siteRepo.save(site);
        finishIndexingInService();
    }

    private void pageCodeNotReceivedStopIndexing(EntityPage page, EntitySite site) {
        site.setStatus(StatusIndexing.FAILED);
        pageRepo.save(page);
        workerDB.updateSiteStatusTime(site);
        finishIndexingInService();
    }

    private void runForkJoinPool(URL urlObj, Document doc, EntitySite site) {
        PagesIndexing parser = new PagesIndexing(urlObj.toString(), site, jsoupCon,
                lemmaFinder, workerUrl, workerDB, indexingService, doc);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(parser);
    }

    private void forkJoinPoolEndedStopIndexing(EntitySite site) {
        if (indexingService.isIndexingStopped()) {
            site.setStatus(StatusIndexing.FAILED);
            site.setLastError(infoErrorIndexingStopped);
        } else {
            site.setStatus(StatusIndexing.INDEXED);
        }
        siteRepo.save(site);
        finishIndexingInService();
    }

    private void finishIndexingInService() {
        int countFinishThreads = indexingService.incrementCountFinishThreads();
        if (countFinishThreads == sitesListObj.getSites().size()) {
            indexingService.setIndexingRunning(false);
            indexingService.setCountFinishThreads(0);
            if (indexingService.isIndexingStopped()) {
                indexingService.setIndexingStopped(false);
            }
        }
    }
}