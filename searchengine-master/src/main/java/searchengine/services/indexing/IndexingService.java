package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexPageURLRequest;
import searchengine.dto.indexing.IndexingResponse;

@Service
public interface IndexingService {
    IndexingResponse startIndexing(IndexPageURLRequest indexPageURLRequest);

    IndexingResponse stopIndexing();

    boolean isIndexingRunning();

    void setIndexingRunning(boolean indexingRunning);

    boolean isIndexingStopped();

    void setIndexingStopped(boolean indexingStopped);

    void setCountFinishThreads(int value);

    int incrementCountFinishThreads();
}