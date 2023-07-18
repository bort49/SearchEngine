package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }


    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity singlePageIndexing(Site site) {
        System.out.println("single parsing: " + site.getUrl());
        return ResponseEntity.ok(indexingService.singlePageIndexing(site));
    }


}
