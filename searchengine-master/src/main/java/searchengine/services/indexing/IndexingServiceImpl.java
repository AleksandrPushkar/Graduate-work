package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.config.Site;
import searchengine.dto.indexing.InfoErrorIndexing;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.indexer.JsoupConnect;
import searchengine.indexer.PageIndexing;
import searchengine.indexer.RunPagesIndexing;
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

    private volatile boolean indexingRunning;
    private volatile boolean indexingStopped;
    private final AtomicInteger countFinishThreads = new AtomicInteger();

    @Override
    public IndexingResponse startIndexing() {
        if (indexingRunning) {
            return new IndexingResponse(false,
                    InfoErrorIndexing.getInfoErrorIndexingAlreadyRunning());
        }

        List<Site> sitesList = sitesListObj.getSites();
        for (Site site : sitesList) {
            new Thread(() -> new RunPagesIndexing(sitesList.size(),
                    jsoupCon, this).indexing(site)).start();
        }

        indexingRunning = true;
        return new IndexingResponse(indexingRunning, null);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!indexingRunning) {
            return new IndexingResponse(false,
                    InfoErrorIndexing.getInfoErrorIndexingNotRunning());
        }

        indexingStopped = true;
        return new IndexingResponse(true, null);
    }

    @Override
    public IndexingResponse pageIndexing(String url) {
        String error = new PageIndexing(jsoupCon, this,
                sitesListObj.getSites()).indexing(url);

        if (error == null) {
            return new IndexingResponse(true, null);
        }

        return new IndexingResponse(false, error);
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
    public void setCountFinishThreads(int value) {
        countFinishThreads.set(value);
    }

    @Override
    public int incrementCountFinishThreads() {
        return countFinishThreads.incrementAndGet();
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
    public int getHttpCodeFromString(String string) {
        String[] digitArray = string.split("\\D+");
        if (digitArray.length == 0)
            return 0;

        return Integer.parseInt(String.join("", digitArray));
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
    public void savePage(EntityPage entityPage) {
        pageRepo.save(entityPage);
    }

    @Override
    public EntityPage synchronizedCheckSavePage(EntityPage page) {
        synchronized (pageRepo) {
            EntityPage foundPage = checkPageInDB(page);
            if (foundPage != null) {
                return foundPage;
            }

            pageRepo.save(page);
            return null;
        }
    }

    @Override
    public EntityPage checkPageInDB(EntityPage pageCheck) {
        Iterable<EntityPage> iterablePages =
                findAllPagesByPath(pageCheck.getPath());

        for (EntityPage pageDB : iterablePages) {
            if (pageDB.getSiteId().getId() == pageCheck.getSiteId().getId()) {
                return pageDB;
            }
        }

        return null;
    }

    @Override
    public Iterable<EntityPage> findAllPagesByPath(String path) {
        return pageRepo.findAllByPath(path);
    }

    @Override
    public int getCountPagesInSite(EntitySite site) {
        return pageRepo.countAllBySiteId(site);
    }

    @Override
    public void deletePage(EntityPage page) {
        pageRepo.delete(page);
    }

    @Override
    public void saveLemma(EntityLemma lemma) {
        lemmaRepo.save(lemma);
    }

    @Override
    public EntityLemma synchronizedCheckSaveLemma(EntityLemma lemma) {
        synchronized (lemmaRepo) {
            EntityLemma foundLemma = checkLemmaInDB(lemma);
            if (foundLemma != null) {
                foundLemma.setFrequency(foundLemma.getFrequency() + 1);
                lemma = foundLemma;
            }

            return lemmaRepo.save(lemma);
        }
    }

    @Override
    public EntityLemma checkLemmaInDB(EntityLemma lemmaCheck) {
        Iterable<EntityLemma> iterableLemmas =
                findAllLemmasByLemma(lemmaCheck.getLemma());

        for (EntityLemma lemmaDB : iterableLemmas) {
            if (lemmaDB.getSiteId().getId() == lemmaCheck.getSiteId().getId()) {
                return lemmaDB;
            }
        }

        return null;
    }

    @Override
    public Iterable<EntityLemma> findAllLemmasByLemma(String lemma) {
        return lemmaRepo.findAllByLemma(lemma);
    }

    @Override
    public int getCountLemmasInSite(EntitySite site) {
        return lemmaRepo.countAllBySiteId(site);
    }

    @Override
    public void deleteLemma(EntityLemma lemma) {
        lemmaRepo.delete(lemma);
    }

    @Override
    public void saveIndex(EntityIndex index) {
        indexRepo.save(index);
    }
}
