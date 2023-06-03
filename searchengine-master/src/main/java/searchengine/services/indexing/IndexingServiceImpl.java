package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.config.Site;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.indexing.IndexingAlreadyRunningException;
import searchengine.exceptions.indexing.IndexingNotRunningException;
import searchengine.workersservices.indexer.JsoupConnect;
import searchengine.workersservices.indexer.PageIndexing;
import searchengine.workersservices.indexer.RunPagesIndexing;
import searchengine.model.*;
import searchengine.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesListObj;
    private final JsoupConnect jsoupCon;
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private final IndexRepository indexRepo;
    private final LemmaRepository lemmaRepo;
    private boolean indexingRunning;
    private boolean indexingStopped;
    private final AtomicInteger countFinishThreads = new AtomicInteger();

    @Override
    public IndexingResponse startIndexing() {
        if (indexingRunning)
            throw new IndexingAlreadyRunningException();

        List<Site> sitesList = sitesListObj.getSites();
        for (Site site : sitesList) {
            new Thread(() -> new RunPagesIndexing(sitesList.size(),
                    jsoupCon, this).indexing(site)).start();
        }

        indexingRunning = true;
        return new IndexingResponse(true, null);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!indexingRunning)
            throw new IndexingNotRunningException();

        indexingStopped = true;
        return new IndexingResponse(true, null);
    }

    @Override
    public IndexingResponse pageIndexing(String url) {
        new PageIndexing(jsoupCon, this,
                sitesListObj.getSites()).indexing(url);

        return new IndexingResponse(true, null);
    }

    @Override
    public boolean isIndexingRunning() {
        return indexingRunning;
    }

    @Override
    public void setIndexingRunning(boolean indexingRunning) {
        this.indexingRunning = indexingRunning;
    }

    @Override
    public boolean isIndexingStopped() {
        return indexingStopped;
    }

    @Override
    public void setIndexingStopped(boolean indexingStopped) {
        this.indexingStopped = indexingStopped;
    }

    @Override
    public void setCountFinishThreads(int value) {
        countFinishThreads.set(value);
    }

    @Override
    public int incrementCountFinishThreads() {
        return countFinishThreads.incrementAndGet();
    }

    @Override
    public EntitySite saveSite(EntitySite site) {
        return siteRepo.save(site);
    }

    @Override
    public EntitySite saveNewSite(Site configSite) {
        EntitySite site = new EntitySite(StatusIndexing.INDEXING,
                configSite.getUrl(), configSite.getName());
        return saveSite(site);
    }

    @Override
    public EntitySite findSiteByUrl(String url) {
        return siteRepo.findByUrl(url);
    }

    @Override
    public void updateSiteStatusTime(EntitySite site) {
        site.setStatusTime(LocalDateTime.now());
        saveSite(site);
    }

    @Override
    public void deleteSite(Site configSite) {
        EntitySite entitySite = findSiteByUrl(configSite.getUrl());
        if (entitySite != null)
            siteRepo.delete(entitySite);
    }

    @Override
    public void savePage(EntityPage page) {
        pageRepo.save(page);
    }

    @Override
    public boolean synchronizedPageSave(EntityPage page) {
        synchronized (pageRepo) {
            boolean exists = pageRepo.existsBySiteAndPath(
                    page.getSite(), page.getPath());

            if (exists)
                return false;

            pageRepo.save(page);
            return true;
        }
    }

    @Override
    public EntityPage findPageBySiteAndPath(EntityPage page) {
        return pageRepo.findBySiteAndPath(
                page.getSite(), page.getPath());
    }

    @Override
    public void deletePage(EntityPage page) {
        pageRepo.delete(page);
    }

    @Override
    public int getCountPagesInSite(EntitySite site) {
        return pageRepo.countAllBySite(site);
    }

    @Override
    public void saveLemma(EntityLemma lemma) {
        lemmaRepo.save(lemma);
    }

    @Override
    public EntityLemma synchronizedLemmaSave(EntityLemma lemma) {
        synchronized (lemmaRepo) {
            EntityLemma foundLemma = lemmaRepo.findBySiteAndLemma(
                    lemma.getSite(), lemma.getLemma());

            if (foundLemma != null) {
                foundLemma.setFrequency(foundLemma.getFrequency() + 1);
                lemma = foundLemma;
            }

            return lemmaRepo.save(lemma);
        }
    }

    @Override
    public void deleteLemma(EntityLemma lemma) {
        lemmaRepo.delete(lemma);
    }

    @Override
    public int getCountLemmasInSite(EntitySite site) {
        return lemmaRepo.countAllBySite(site);
    }

    @Override
    public void saveIndex(EntityIndex index) {
        indexRepo.save(index);
    }
}
