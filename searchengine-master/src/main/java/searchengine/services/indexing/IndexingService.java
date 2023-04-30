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
    void setCountFinishThreads(int value);
    int incrementCountFinishThreads();
    boolean isIndexingStopped();
    void setIndexingStopped(boolean indexingStopped);
    int getHttpCodeFromString(String string);
    EntitySite saveSite(EntitySite site);
    EntitySite saveNewSite(Site site);
    EntitySite findSiteByUrl(String url);
    void updateSiteStatusTime(EntitySite site);
    void deleteSite(Site configSite);
    void savePage(EntityPage page);
    EntityPage synchronizedCheckSavePage(EntityPage page);
    EntityPage checkPageInDB(EntityPage page);
    Iterable<EntityPage> findAllPagesByPath(String path);
    int getCountPagesInSite(EntitySite site);
    void deletePage(EntityPage page);
    void saveLemma(EntityLemma lemma);
    EntityLemma synchronizedCheckSaveLemma(EntityLemma lemma);
    EntityLemma checkLemmaInDB(EntityLemma lemma);
    Iterable<EntityLemma> findAllLemmasByLemma(String lemma);
    int getCountLemmasInSite(EntitySite site);
    void deleteLemma(EntityLemma lemma);
    void saveIndex(EntityIndex index);
}