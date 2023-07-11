package searchengine.indexer;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exceptions.indexing.InvalidUrlException;
import searchengine.exceptions.indexing.PageCodeNotReceivedException;
import searchengine.exceptions.indexing.SiteNotFoundException;
import searchengine.model.*;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingService;

import java.net.URL;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PageIndexing {
    private final SitesList sitesListObj;
    private final JsoupConnect jsoupCon;
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private final LemmaRepository lemmaRepo;
    private final WorkingWithUrl workingUrl;
    private final WorkingWithDatabase workerDB;
    private final IndexingService indexingService;

    public void indexing(String url) {
        String pageUrl = workingUrl.urlCorrection(url);
        if (pageUrl == null || workingUrl.checkForDisqualification(pageUrl)) {
            indexingService.setIndexingRunning(false);
            throw new InvalidUrlException();
        }
        Site configSite = findSiteInList(pageUrl);
        EntityPage page = new EntityPage(workingUrl.cutUrlToPath(pageUrl));
        Document doc = jsoupCon.getPageCode(pageUrl, page, null);
        if (doc == null) {
            indexingService.setIndexingRunning(false);
            throw new PageCodeNotReceivedException(page.getCode());
        }
        EntitySite site = addPageInDB(configSite, page);
        workerDB.loopSaveLemmasAndIndexes(doc, page, site);
    }

    private Site findSiteInList(String pageUrl) {
        URL pageUrlObj = workingUrl.getURL(pageUrl);
        List<Site> sitesList = sitesListObj.getSites();
        for (Site site : sitesList) {
            String siteUrl = workingUrl.urlCorrection(site.getUrl());
            URL siteUrlObj = workingUrl.getURL(siteUrl);
            if (siteUrlObj == null) {
                continue;
            }
            String pageHost = pageUrlObj.getHost();
            String siteHost = siteUrlObj.getHost();
            if (pageHost.equals(siteHost)) {
                return site;
            }
        }
        indexingService.setIndexingRunning(false);
        throw new SiteNotFoundException();
    }

    private EntitySite addPageInDB(Site configSite, EntityPage page) {
        EntitySite site = siteRepo.findByUrl(configSite.getUrl());
        if (site == null) {
            site = workerDB.saveNewSite(configSite);
            page.setSite(site);
            pageRepo.save(page);
            return site;
        }
        page.setSite(site);
        EntityPage foundPage = pageRepo.findByPathAndSite(page.getPath(), page.getSite());
        if (foundPage != null) {
            deleteFoundPage(foundPage);
        }
        pageRepo.save(page);
        return site;
    }

    private void deleteFoundPage(EntityPage page) {
        Set<EntityIndex> indexes = page.getIndexes();
        if (indexes == null) {
            pageRepo.delete(page);
            return;
        }
        List<EntityLemma> lemmas = indexes.stream().map(EntityIndex::getLemma).toList();
        pageRepo.delete(page);
        for (EntityLemma lemma : lemmas) {
            if (lemma.getFrequency() > 1) {
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaRepo.save(lemma);
                continue;
            }
            lemmaRepo.delete(lemma);
        }
    }
}
