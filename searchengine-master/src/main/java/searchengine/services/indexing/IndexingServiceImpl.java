package searchengine.services.indexing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.config.Site;
import searchengine.dto.indexing.IndexPageURLRequest;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.common.ConfigSitesNotFoundException;
import searchengine.exceptions.indexing.IndexingAlreadyRunningException;
import searchengine.exceptions.indexing.IndexingNotRunningException;
import searchengine.indexer.PageIndexing;
import searchengine.indexer.RunPagesIndexing;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesListObj;
    private final PageIndexing pageIndexing;
    private final RunPagesIndexing runPagesIndexing;
    private boolean indexingRunning;
    private boolean indexingStopped;
    private final AtomicInteger countFinishThreads = new AtomicInteger();

    @Autowired
    public IndexingServiceImpl(SitesList sitesListObj, @Lazy PageIndexing pageIndexing,
                               @Lazy RunPagesIndexing runPagesIndexing) {
        this.sitesListObj = sitesListObj;
        this.pageIndexing = pageIndexing;
        this.runPagesIndexing = runPagesIndexing;
    }

    @Override
    public IndexingResponse startIndexing(IndexPageURLRequest indexPageURLRequest) {
        List<Site> sitesList = sitesListObj.getSites();
        if(sitesList.isEmpty()) {
            throw new ConfigSitesNotFoundException();
        }
        if (indexingRunning) {
            throw new IndexingAlreadyRunningException();
        }
        if(indexPageURLRequest != null) {
            indexingRunning = true;
            pageIndexing.indexing(indexPageURLRequest.getUrl());
            indexingRunning = false;
            return new IndexingResponse(true);
        }
        indexingRunning = true;
        for (Site site : sitesList) {
            new Thread(() -> runPagesIndexing.indexing(site)).start();
        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!indexingRunning) {
            throw new IndexingNotRunningException();
        }
        indexingStopped = true;
        return new IndexingResponse(true);
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
}
