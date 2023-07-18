package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageEntity;
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

  //  static private volatile HashSet<String> tmpPassedUrlsList = new HashSet<>(); //uses before save urls to base
    static private Set<String> tmpPassedUrlsList = ConcurrentHashMap.newKeySet();  //uses before save urls to database


    @Override
    public IndexingResponse singlePageIndexing(Site site) {
        IndexingResponse response = new IndexingResponse(false);

        if (indexingThreadsStartedCounter > 0) {
            response.setError("Индексация уже запущена");
            return response;
        }

        if (site.getUrl().isEmpty()) {
            response.setError("Задайте адрес сайта");
            return response;
        }

        if (site.getName() == null || site.getName().isEmpty()) {
            String name = site.getUrl().replace("http://", "");
            name = name.replace("https://", "");
            name = name.replace("www", "");
            site.setName(name);
        }


        tmpPassedUrlsList.clear();
        stopIndexingRequest = false;
        pagesCounter = 0;
        indexingThreadsStartedCounter = 0;
        referrerValue = sites.getReferrer();
        userAgentValue = sites.getUserAgent();

        deleteCurrentSiteInfo(site);
        SiteEntity siteEntity = newSiteRecord(site);
        Thread thread = new Thread(new Thread(() -> getAllSitePages(siteEntity)));

        thread.setName("site-1");
        thread.start();
        indexingThreadsStartedCounter++;
        response.setResult(true);
        return response;
    }


    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse(false);

        if (indexingThreadsStartedCounter > 0) {
            response.setError("Индексация уже запущена");
            return response;
        }

        tmpPassedUrlsList.clear();
        stopIndexingRequest = false;
        pagesCounter = 0;
        indexingThreadsStartedCounter = 0;

        final List<Site> sitesList = sites.getSites();
        referrerValue = sites.getReferrer();
        userAgentValue = sites.getUserAgent();

        Thread[] threads = new Thread[sitesList.size()]; //запускаем каждый сайт в отдельном потоке

       // long heapSize = Runtime.getRuntime().totalMemory();
        //Print the jvm heap size.
       // System.out.println("Heap Size = " + heapSize);


        for (int i = 0; i < sitesList.size(); i++) {
             Site site = sitesList.get(i);
            System.out.println("delete all by site " + site.getName());
             deleteCurrentSiteInfo(site);


            SiteEntity siteEntity = newSiteRecord(site);
//             Set<String> linksList = new TreeSet<>(SiteParser.getAllLinks(sitesList.get(i).getUrl())); //"https://skillbox.ru"
//            new Thread(() -> SiteParser.getAllLinks(site.getUrl()));
            threads[i] = new Thread(new Thread(() -> getAllSitePages(siteEntity)));

            threads[i].setName("site-"+i);
            threads[i].start();
            indexingThreadsStartedCounter++;
        }

        //waiting for threads to finish
   //     for ( Thread t : threads) {   //!!!!!!! ждет по порядку запуска, т.е. пока не закончится второй - skillbox, не дает результат по короткому 3-ему который уже завершился
   //         try {
   //             t.join(); //остановка - ожидание завершения процесса
  //          } catch (InterruptedException e) {
  //              throw new RuntimeException(e);
  //          }
  //      }

  //      System.out.println("Indexing completed");

 //       indexingStarted = false;
        response.setResult(true);
        return response;
    }

    private void getAllSitePages(SiteEntity siteEntity) {
        String siteUrl = siteEntity.getUrl().replace("://www.", "://");
        if (!siteUrl.endsWith("/")) { siteUrl+="/"; } //absUrl
        siteEntity.setDomainName(siteUrl.substring(0,siteUrl.length()-1));


        long startTime = System.currentTimeMillis();
        System.out.println( siteUrl + " - start parsing");

        try {
            new ForkJoinPool(availableCores).invoke(new GetAllLinksFromPage(siteUrl, pageRepository, siteEntity, tmpPassedUrlsList));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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





}
