package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
//import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

//    private final Random random = new Random();
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;



    @Override
    public StatisticsResponse getStatistics() {
//        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
//        String[] errors = {
//                "Ошибка индексации: главная страница сайта не доступна",
//                "Ошибка индексации: сайт не доступен",
//                ""
//        };

        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        Iterable<SiteEntity> siteIterable = siteRepository.findAll();
        ArrayList<SiteEntity> sitesList = new ArrayList<>();
        for (SiteEntity site: siteIterable) {
            sitesList.add(site);
        }


        total.setSites(sitesList.size());
        total.setIndexing((IndexingServiceImpl.indexingThreadsStartedCounter > 0) ? true : false);

        for (int i = 0; i < sitesList.size(); i++) {
             SiteEntity site = sitesList.get(i);
             DetailedStatisticsItem item = new DetailedStatisticsItem();
             item.setName(site.getName());
             item.setUrl(site.getUrl());


             int pages = pageRepository.countBySite_id(site.getId());
             int lemmas = lemmaRepository.countBySite_id(site.getId());
             item.setPages(pages);
             item.setLemmas(lemmas);

             SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
             item.setStatus(siteEntity.getStatus().toString());
             item.setError(siteEntity.getLastError());
             ZoneId zone = ZoneId.of(sites.getCurrentTimeZone());
             ZoneOffset zoneOffSet = zone.getRules().getOffset(siteEntity.getStatusTime());
             item.setStatusTime(siteEntity.getStatusTime().toEpochSecond(zoneOffSet) * 1000);

             total.setPages(total.getPages() + pages);
             total.setLemmas(total.getLemmas() + lemmas);
             detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
