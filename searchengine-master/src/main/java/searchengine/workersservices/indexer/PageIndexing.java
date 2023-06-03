package searchengine.workersservices.indexer;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import searchengine.config.Site;
import searchengine.exceptions.indexing.InvalidUrlException;
import searchengine.exceptions.indexing.LemmasFinderNotReadyWorkException;
import searchengine.exceptions.indexing.PageCodeNotReceivedException;
import searchengine.exceptions.indexing.SiteNotFoundException;
import searchengine.model.*;
import searchengine.services.indexing.IndexingService;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class PageIndexing {
    private final JsoupConnect jsoupCon;
    private final IndexingService indexingService;
    private final List<Site> sitesList;
    private String urlPage;
    private Site siteConfig;
    private EntitySite site;
    private EntityPage page;

    public void indexing(String url) {
        urlPage = WorkUrl.urlCorrection(url);
        if (urlPage == null || WorkUrl.checkForDisqualification(urlPage))
            throw new InvalidUrlException();

        findSiteInList();
        if (siteConfig == null)
            throw new SiteNotFoundException();

        page = new EntityPage(WorkUrl.cutUrlToPath(urlPage));
        Document doc = jsoupCon.getPageCode(urlPage, page, null);
        if (doc == null)
            throw new PageCodeNotReceivedException(page.getCode());

        LemmaFinder lemmaFinder = new LemmaFinder();
        if (lemmaFinder.getLuceneMorphology() == null)
            throw new LemmasFinderNotReadyWorkException();

        addPageInDB();
        Map<String, Integer> lemmasPage = lemmaFinder.getLemmasPages(doc);
        for (String strLemma : lemmasPage.keySet()) {
            EntityLemma lemma = new EntityLemma(site, strLemma, 1);
            lemma = indexingService.synchronizedLemmaSave(lemma);
            int quantity = lemmasPage.get(strLemma);
            EntityIndex index = new EntityIndex(page, lemma, quantity);
            indexingService.saveIndex(index);
        }
    }

    private void findSiteInList() {
        URL urlPageObj = WorkUrl.getURL(urlPage);
        if (urlPageObj == null)
            return;

        for (Site site : sitesList) {
            URL urlSiteObj = WorkUrl.getURL(site.getUrl());
            if (urlSiteObj == null)
                continue;

            String hostPage = urlPageObj.getHost();
            String hostSite = urlSiteObj.getHost();
            if (hostPage.equals(hostSite)) {
                siteConfig = site;
                break;
            }
        }
    }

    private void addPageInDB() {
        site = indexingService
                .findSiteByUrl(siteConfig.getUrl());
        if (site == null) {
            site = indexingService.saveNewSite(siteConfig);
            page.setSite(site);
            indexingService.savePage(page);
            return;
        }

        page.setSite(site);
        EntityPage foundPage = indexingService.findPageBySiteAndPath(page);
        if (foundPage != null)
            deleteFoundPage(foundPage);

        indexingService.savePage(page);
    }

    private void deleteFoundPage(EntityPage page) {
        Set<EntityIndex> indexes = page.getIndexes();
        if (indexes == null) {
            indexingService.deletePage(page);
            return;
        }

        List<EntityLemma> lemmas = indexes.stream()
                .map(EntityIndex::getLemma).toList();

        indexingService.deletePage(page);
        for (EntityLemma lemma : lemmas) {
            if (lemma.getFrequency() > 1) {
                lemma.setFrequency(lemma.getFrequency() - 1);
                indexingService.saveLemma(lemma);
                continue;
            }

            indexingService.deleteLemma(lemma);
        }
    }
}
