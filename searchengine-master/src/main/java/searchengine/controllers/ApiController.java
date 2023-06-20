package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexPageURLRequest;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;
import searchengine.dto.search.SearchOptionsRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
            return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing(null));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        IndexPageURLRequest indexPageURLRequest = new IndexPageURLRequest(url);
        return ResponseEntity.ok(indexingService.startIndexing(indexPageURLRequest));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam(defaultValue = "0") int offset,
                                                 @RequestParam(defaultValue = "20") int limit,
                                                 @RequestParam(required = false) String site) {
        SearchOptionsRequest searchOptionsRequest = new SearchOptionsRequest(query, offset, limit, site);
        return ResponseEntity.ok(searchService.search(searchOptionsRequest));
    }
}
