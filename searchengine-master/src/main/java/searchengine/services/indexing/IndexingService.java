package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.EntityIndex;
import searchengine.model.EntityLemma;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;

@Service
public interface IndexingService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    IndexingResponse pageIndexing(String url);
    boolean isIndexingRunning();
    void setIndexingRunning(boolean indexingRunning);
    boolean isIndexingStopped();
    void setIndexingStopped(boolean indexingStopped);
    void setCountFinishThreads(int value);
    int incrementCountFinishThreads();
    EntitySite saveSite(EntitySite site);
    EntitySite saveNewSite(Site site);
    EntitySite findSiteByUrl(String url);
    void updateSiteStatusTime(EntitySite site);
    void deleteSite(Site configSite);
    void savePage(EntityPage page);
    boolean synchronizedPageSave(EntityPage page);
    EntityPage findPageBySiteAndPath(EntityPage page);
    void deletePage(EntityPage page);
    int getCountPagesInSite(EntitySite site);
    void saveLemma(EntityLemma lemma);
    EntityLemma synchronizedLemmaSave(EntityLemma lemma);
    void deleteLemma(EntityLemma lemma);
    int getCountLemmasInSite(EntitySite site);
    void saveIndex(EntityIndex index);
}