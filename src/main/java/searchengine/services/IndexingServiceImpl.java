package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;


import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import static java.lang.Thread.sleep;


@Service
@RequiredArgsConstructor // создает конструктор с аргументами, соответствующими неинициализированным final-полям класса.
public class IndexingServiceImpl implements IndexingService{

    public static boolean stopIndexingRequest;
    static int indexingThreadsStartedCounter = 0;
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    public static String userAgentValue;
    public static String referrerValue;

    static int availableCores = Runtime.getRuntime().availableProcessors();

    static int pagesCounter;

    static private Set<String> tmpPassedUrlsList = ConcurrentHashMap.newKeySet();  //it's used before saving urls to database



    @Override
    public IndexingResponse startIndexing(List<Site> sitesList) {
        IndexingResponse response = new IndexingResponse(false);

        if (indexingThreadsStartedCounter > 0) {
            response.setError("Indexing is already started");
            return response;
        }

        tmpPassedUrlsList.clear();
        stopIndexingRequest = false;
        pagesCounter = 0;
        indexingThreadsStartedCounter = 0;

     //   final List<Site> sitesList = sites.getSites();
        referrerValue = sites.getReferrer();
        userAgentValue = sites.getUserAgent();

        Thread[] threads = new Thread[sitesList.size()]; //запускаем каждый сайт в отдельном потоке

        for (int i = 0; i < sitesList.size(); i++) {
             Site site = sitesList.get(i);

            if (site.getUrl().isEmpty()) {
                response.setError("Set the address of the site");
                return response;
            }


            urlVarietyControl(site);


            System.out.println("delete all by site " + site.getName());
             deleteCurrentSiteInfo(site);


            SiteEntity siteEntity = newSiteRecord(site);
            threads[i] = new Thread(new Thread(() -> getAllSitePages(siteEntity)));

            threads[i].setName("site-"+i);
            threads[i].start();
            indexingThreadsStartedCounter++;
        }

        response.setResult(true);
        return response;
    }

    private void getAllSitePages(SiteEntity siteEntity) {
        String siteUrl = siteEntity.getUrl();
        if (!siteUrl.endsWith("/")) { siteUrl+="/"; } //absUrl
        siteEntity.setDomainName(siteUrl.substring(0,siteUrl.length()-1));


        long startTime = System.currentTimeMillis();
        System.out.println( siteUrl + " - start parsing");

        try {

            new ForkJoinPool(availableCores).invoke(new GetAllLinksFromPage(siteUrl, siteRepository, pageRepository, siteEntity, tmpPassedUrlsList));

            if (siteEntity.getStatus() != Status.FAILED) {
                siteEntity.setStatus(Status.INDEXED);
            }
            if (stopIndexingRequest == true) {
                siteEntity.setLastError("Manual stop indexing!");
            }

        } catch (Exception e) {
           // throw new RuntimeException(e);
             siteEntity.setStatus(Status.FAILED);
             siteEntity.setLastError(e.getMessage());
        }

        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
        System.out.println("\n" + siteUrl + " - parsing completed. Lead time(min): " + (System.currentTimeMillis() - startTime) / 60000);

        indexingThreadsStartedCounter--;
        if (indexingThreadsStartedCounter <= 0) {
            System.out.println("Indexing completed");
        }
    }


    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse(false);
        if (indexingThreadsStartedCounter > 0) {
            stopIndexingRequest = true;
            response.setResult(true);
        }
        else
           response.setError("Индексация не запущена");

        return response;
    }



    private SiteEntity newSiteRecord(Site site) {
      SiteEntity siteEntity = new SiteEntity();
      siteEntity.setName(site.getName());
      siteEntity.setUrl(site.getUrl());
      siteEntity.setStatus(Status.INDEXING);
      siteEntity.setStatusTime(LocalDateTime.now());
      siteEntity.setLastError("Ошибок не обнаружено");
      return siteRepository.save(siteEntity);
    }


    private void deleteCurrentSiteInfo(Site site) {
      SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
      if (siteEntity != null) {
  //        pageRepository.deleteAllBySite_id(siteEntity.getId());
          siteRepository.delete(siteEntity); // + Cascade
          return;
      }

      siteEntity = siteRepository.findByName(site.getName());
      if (siteEntity != null) {
     //     pageRepository.deleteAllBySite_id(siteEntity.getId());
          siteRepository.delete(siteEntity);
      }

    }

    private void updateSiteRecord(Site site, Status status, String errorString) {
        Optional<SiteEntity> optional = siteRepository.findById(site.getId());
        if (optional.isPresent()) {
            SiteEntity siteEntity = optional.get();
            siteEntity.setStatus(status);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteEntity.setLastError(errorString);
            siteRepository.save(siteEntity);
        }
    }

    private void urlVarietyControl(Site site) {
        if (site.getUrl().startsWith("www.")) {
            site.setUrl(site.getUrl().replace("www.", "https://"));
        }

        if (!site.getUrl().startsWith("http")) {
            site.setUrl("https://" + site.getUrl());
        }

        site.setUrl(site.getUrl().replace("://www.", "://"));

        if (site.getName() == null || site.getName().isEmpty()) {
            String name = site.getUrl().replace("http://", "");
            name = name.replace("https://", "");
            name = name.replace("www.", "");
            site.setName(name);
        }

    }



}
