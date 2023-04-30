package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.statistics.StatisticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        StatisticsResponse response = statisticsService.getStatistics();

        if (response.getError() == null)
            return ResponseEntity.ok(response);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse response = indexingService.startIndexing();

        if (response.getError() == null)
            return ResponseEntity.ok(response);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse response = indexingService.stopIndexing();

        if (response.getError() == null)
            return ResponseEntity.ok(response);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(
            @RequestParam(value = "url") String url) {
        IndexingResponse response = indexingService.pageIndexing(url);

        if (response.getError() == null)
            return ResponseEntity.ok(response);

        String error = response.getError();

        if(error.contains("Внутренняя"))
            return ResponseEntity.status(HttpStatus
                    .INTERNAL_SERVER_ERROR).body(response);

        int code = indexingService.getHttpCodeFromString(error);
        if (code != 0)
            return ResponseEntity.status(code).body(response);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
