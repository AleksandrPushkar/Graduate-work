package searchengine.indexer;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import searchengine.config.Site;
import searchengine.model.*;
import searchengine.services.indexing.IndexingService;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class RunPagesIndexing {
    private String url;
    private URL urlObj;
    private final int numberSites;
    private EntitySite site;
    private EntityPage page;
    private final JsoupConnect jsoupCon;
    private final IndexingService indexingService;
    private final LemmaFinder lemmaFinder = new LemmaFinder();

    private final String infoErrorInvalidUrl = "Некорректный URL " +
            "или не соответствует шаблону \"URL для индексации\"";

    private final String infoErrorIndexingStopped = "Индексация " +
            "остановлена пользователем";

    public void indexing(Site configSite) {
        indexingService.deleteSite(configSite);
        site = indexingService.saveNewSite(configSite);
        if (!checkUrlSite())
            return;

        if (lemmaFinder.getLuceneMorphology() == null) {
            lemmaFinderNotReadyWorkStopIndexing();
            return;
        }

        page = new EntityPage(site, urlObj.getPath());
        Document doc = jsoupCon.getPageCode(url, page, site);
        if (doc == null) {
            pageCodeNotReceivedStopIndexing();
            return;
        }

        indexingService.savePage(page);
        indexingService.updateSiteStatusTime(site);
        loopAddingLemmasAndIndexesInDB(doc);
        if (!indexingService.isIndexingStopped()) {
            runForkJoinPool(doc);
        }

        forkJoinPoolEndedStopIndexing();
    }

    private boolean checkUrlSite() {
        url = WorkUrl.urlCorrection(site.getUrl());
        urlObj = WorkUrl.getURL(url);
        if (urlObj == null || WorkUrl.checkForDisqualification(url)) {
            urlFailsValidationStopIndexing();
            return false;
        }

        return true;
    }

    private void urlFailsValidationStopIndexing() {
        site.setStatus(StatusIndexing.FAILED);
        site.setLastError(infoErrorInvalidUrl);
        indexingService.saveSite(site);
        finishIndexingInService();
    }

    private void lemmaFinderNotReadyWorkStopIndexing() {
        site.setStatus(StatusIndexing.FAILED);
        site.setLastError(lemmaFinder.getTextError());
        indexingService.saveSite(site);
        finishIndexingInService();
    }

    private void pageCodeNotReceivedStopIndexing() {
        site.setStatus(StatusIndexing.FAILED);
        indexingService.savePage(page);
        indexingService.updateSiteStatusTime(site);
        finishIndexingInService();
    }

    private void loopAddingLemmasAndIndexesInDB(Document doc) {
        Map<String, Integer> lemmasPage = lemmaFinder.getLemmasPages(doc);
        for (String strLemma : lemmasPage.keySet()) {
            if (indexingService.isIndexingStopped())
                return;

            EntityLemma lemma = new EntityLemma(site, strLemma, 1);
            lemma = indexingService.synchronizedCheckSaveLemma(lemma);
            int quantity = lemmasPage.get(strLemma);
            EntityIndex index = new EntityIndex(page, lemma, quantity);
            indexingService.saveIndex(index);
        }
    }

    private void runForkJoinPool(Document doc) {
        PagesIndexing parser = new PagesIndexing(url,
                doc, site, jsoupCon, lemmaFinder, indexingService);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(parser);
    }

    private void forkJoinPoolEndedStopIndexing() {
        if (indexingService.isIndexingStopped()) {
            site.setStatus(StatusIndexing.FAILED);
            site.setLastError(infoErrorIndexingStopped);
        } else
            site.setStatus(StatusIndexing.INDEXED);
        indexingService.saveSite(site);
        finishIndexingInService();
    }

    private void finishIndexingInService() {
        int countFinishThreads =
                indexingService.incrementCountFinishThreads();

        if (countFinishThreads == numberSites) {
            indexingService.setIndexingRunning(false);
            indexingService.setCountFinishThreads(0);
            if (indexingService.isIndexingStopped())
                indexingService.setIndexingStopped(false);
        }
    }
}