package searchengine.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    private final SitesList sites;


    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SitesList sites) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.sites = sites;
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
    public ResponseEntity singlePageIndexing(String url) {
//        List<Site> sitesList = new ArrayList<>();
//        sitesList.add(site);
        System.out.println(url);

        return ResponseEntity.ok(indexingService.pageIndexing(url));
    }


}
