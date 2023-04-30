package searchengine.indexer;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import searchengine.config.Site;
import searchengine.dto.indexing.InfoErrorIndexing;
import searchengine.model.*;
import searchengine.services.indexing.IndexingService;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class PageIndexing {
    private String urlPage;
    private Site siteConfig;
    private EntitySite site;
    private EntityPage page;
    private EntityPage foundPage;
    private final JsoupConnect jsoupCon;
    private final IndexingService indexingService;
    private final LemmaFinder lemmaFinder = new LemmaFinder();
    private final List<Site> sitesList;
    private final HashSet<EntityLemma> lemmasToDelete = new HashSet<>();

    public String indexing(String url) {
        urlPage = WorkUrl.urlCorrection(url);
        if (urlPage == null || WorkUrl.checkForDisqualification(urlPage))
            return InfoErrorIndexing.getInfoErrorInvalidUrl();

        findSiteInList();
        if (siteConfig == null)
            return InfoErrorIndexing.getInfoErrorSiteNotFound();

        page = new EntityPage(WorkUrl.cutUrlToPath(urlPage));
        Document doc = jsoupCon.getPageCode(urlPage, page, null);
        if (doc == null)
            return InfoErrorIndexing
                    .getInfoErrorPageCodeNotReceived(page.getCode());

        if (lemmaFinder.getLuceneMorphology() == null)
            return InfoErrorIndexing.getInfoErrorLemmaFinderNotReadyWork();

        addPageInDB();
        Map<String, Integer> lemmasPage = lemmaFinder.getLemmasPages(doc);
        for (String strLemma : lemmasPage.keySet()) {
            EntityLemma lemma = new EntityLemma(site, strLemma, 1);
            lemma = indexingService.synchronizedCheckSaveLemma(lemma);
            int quantity = lemmasPage.get(strLemma);
            EntityIndex index = new EntityIndex(page, lemma, quantity);
            indexingService.saveIndex(index);
        }

        return null;
    }

    private void addPageInDB() {
        site = indexingService
                .findSiteByUrl(siteConfig.getUrl());
        if (site == null) {
            site = indexingService.saveNewSite(siteConfig);
            page.setSiteId(site);
            indexingService.savePage(page);
            return;
        }

        page.setSiteId(site);
        foundPage = indexingService.synchronizedCheckSavePage(page);
        if (foundPage != null) {
            deleteFoundPage();
            indexingService.savePage(page);
        }
    }

    private void deleteFoundPage() {
        updateLemmasFoundPage();
        indexingService.deletePage(foundPage);
        deleteLemmasFoundPage();
    }

    private void updateLemmasFoundPage() {
        for (EntityIndex index : foundPage.getIndexes()) {
            EntityLemma lemma = index.getLemmaId();
            if (lemma.getFrequency() > 1) {
                lemma.setFrequency(lemma.getFrequency() - 1);
                indexingService.saveLemma(lemma);
                continue;
            }

            lemmasToDelete.add(lemma);
        }
    }

    private void deleteLemmasFoundPage() {
        for (EntityLemma lemma : lemmasToDelete) {
            indexingService.deleteLemma(lemma);
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
}
