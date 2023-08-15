package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
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

    private static final int PARTIAL_COUNT_FOR_SAVE_RECORDS = 50_000;

    public static boolean stopIndexingRequest;
    static int indexingThreadsStartedCounter = 0;
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;
    public static String userAgentValue;
    public static String referrerValue;

    static int availableCores = Runtime.getRuntime().availableProcessors();

    static int pagesCounter;

    static private Set<String> tmpPassedUrlsList = ConcurrentHashMap.newKeySet();  //it's used before saving urls to database

//    static private ConcurrentHashMap<String, Integer> tmpSiteLemmaList = new ConcurrentHashMap<>();
//    ArrayList<ConcurrentHashMap> tmpSitesLemmaList;


    @Override
    public IndexingResponse pageIndexing(String url) {
        IndexingResponse response = new IndexingResponse(false);

        url = urlVarietyControl(url);


        referrerValue = sites.getReferrer();
        userAgentValue = sites.getUserAgent();
        final List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            site.setUrl(urlVarietyControl(site.getUrl()));


            if (url.startsWith(site.getUrl())) {

                SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
                if (siteEntity == null) {
                    newSiteRecord(site);
                }

                siteEntity.setDomainName(site.getUrl().substring(0,site.getUrl().length()-1));

                String pageUrlForSave =  url.substring(siteEntity.getDomainName().length());

                Optional<PageEntity> optionalPageEntity = pageRepository.findFirstByPathAndSite_id(pageUrlForSave, siteEntity.getId());
                if (optionalPageEntity.isPresent()) {
                    PageEntity page = optionalPageEntity.get();
                    pageRepository.delete(page);
                    System.out.println("deleted current record from page table");
                }


              //  SiteEntity finalSiteEntity = siteEntity;
        //        new PageParseAndIndexing(url, true, siteRepository, pageRepository, siteEntity, tmpPassedUrlsList);
                new ForkJoinPool(availableCores).invoke(new PageParseAndIndexing(url, true, siteRepository, pageRepository, lemmaRepository, indexRepository, siteEntity, tmpPassedUrlsList));



                break;
            }

            if (i == sitesList.size()-1) {
                response.setError("This page is outside the sites specified in the configuration file");
                return response;

            }

        }


        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse(false);

        if (indexingThreadsStartedCounter > 0) {
            response.setError("Indexing is already started");
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
        List<ConcurrentHashMap<String, Integer>> tmpSitesLemmaSet = new ArrayList<ConcurrentHashMap<String, Integer>>();

        for (int i = 0; i < sitesList.size(); i++) {
             Site site = sitesList.get(i);

            if (site.getUrl().isEmpty()) {
                response.setError("Set the address of the site");
                return response;
            }


            site.setUrl(urlVarietyControl(site.getUrl()));
            if (site.getName().isEmpty() || site.getName() == null) {
                site.setName(createSiteNameByUrl(site.getUrl()));
            }

            System.out.println("delete all data by site " + site.getName());
            deleteCurrentSiteInfo(site);


            SiteEntity siteEntity = newSiteRecord(site);
//            ConcurrentHashMap<String, Integer> tmpSiteLemmaSet = new ConcurrentHashMap<>();
            threads[i] = new Thread(new Thread(() -> siteIndexing(siteEntity)));

            threads[i].setName("site-"+i);
            threads[i].start();
            indexingThreadsStartedCounter++;
        }

        response.setResult(true);
        return response;
    }

    private void siteIndexing(SiteEntity siteEntity) {
        String siteUrl = siteEntity.getUrl();
        if (!siteUrl.endsWith("/")) { siteUrl+="/"; } //absUrl
        siteEntity.setDomainName(siteUrl.substring(0,siteUrl.length()-1));


        long startTime = System.currentTimeMillis();
        System.out.println("\n" + siteUrl + " - start parsing");

        try {

            new ForkJoinPool(availableCores).invoke(new PageParseAndIndexing(siteUrl, false, siteRepository, pageRepository, lemmaRepository, indexRepository, siteEntity, tmpPassedUrlsList));

            if (siteEntity.getStatus() != Status.FAILED) {
                siteEntity.setStatus(Status.INDEXED);
            }
            if (stopIndexingRequest == true) {
                siteEntity.setLastError("\nManual stop indexing!");
            }

        } catch (Exception e) {
           // throw new RuntimeException(e);
             siteEntity.setStatus(Status.FAILED);
             siteEntity.setLastError(e.getMessage());
        }


        saveLemmaAndIndexesSetsBySite(siteEntity);



        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
        System.out.println("\n" + siteUrl + " - parsing completed. Lead time(min): " + (System.currentTimeMillis() - startTime) / 60000);

        indexingThreadsStartedCounter--;
        if (indexingThreadsStartedCounter <= 0) {
            System.out.println("\nIndexing completed");
        }
    }



    private void saveLemmaAndIndexesSetsBySite(SiteEntity siteEntity){
  /*
        System.out.println("\nSave Lemma set by " + siteEntity.getUrl());
        ArrayList<LemmaEntity> siteLemmaEntityList = new ArrayList<>(siteEntity.siteLemmaSet.values());
        lemmaRepository.saveAll(siteLemmaEntityList);
        //lemmaRepository.flush();
        System.out.println("\nlemmas by site " + siteEntity.getUrl() + " saved");


        System.out.println("\nSave indexes set by " + siteEntity.getUrl());
        System.out.println("Records count for save: " + siteEntity.siteIndexSet.size());
        indexRepository.saveAll(siteEntity.siteIndexSet);
        //indexRepository.flush();

        System.out.println("\nAll indexes by site " + siteEntity.getUrl() + " saved");
*/

        //for big data
        System.out.println("\nSave Lemma set by " + siteEntity.getUrl());
        ArrayList<LemmaEntity> siteLemmaEntityList = new ArrayList<>(siteEntity.siteLemmaSet.values());
        List<List<LemmaEntity>> partitionsForSaveLemmas = new ArrayList<>();

        for (int i = 0; i < siteLemmaEntityList.size(); i+=PARTIAL_COUNT_FOR_SAVE_RECORDS) {
            partitionsForSaveLemmas.add(siteLemmaEntityList.subList(i, Math.min(i + PARTIAL_COUNT_FOR_SAVE_RECORDS, siteLemmaEntityList.size())));
        }

        for (List<LemmaEntity> portion: partitionsForSaveLemmas) {
            lemmaRepository.saveAllAndFlush(portion);
        }

        partitionsForSaveLemmas.clear();
        System.out.println("\nlemmas by site " + siteEntity.getUrl() + " saved");


        System.out.println("\nSave indexes set by " + siteEntity.getUrl());
        System.out.println("Records count for save: " + siteEntity.siteIndexSet.size());
        List<List<IndexEntity>> partitionsForSaveIndexes = new ArrayList<>();

        for (int i = 0; i < siteEntity.siteIndexSet.size(); i+=PARTIAL_COUNT_FOR_SAVE_RECORDS) {
            partitionsForSaveIndexes.add(siteEntity.siteIndexSet.subList(i, Math.min(i + PARTIAL_COUNT_FOR_SAVE_RECORDS, siteEntity.siteIndexSet.size())));
        }

        for (List<IndexEntity> portion: partitionsForSaveIndexes) {
            indexRepository.saveAll(portion);
            System.out.println("partially saved:  " + indexRepository.count() + " records in Index. " + LocalDateTime.now());
        }

     //indexRepository.flush();

        partitionsForSaveIndexes.clear();
        System.out.println("\nAll indexes by site " + siteEntity.getUrl() + " saved");

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

 /*   private void urlVarietyControl(Site site) {
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
*/
 private String urlVarietyControl(String url) {
     if (url.startsWith("www.")) {
        url.replace("www.", "https://");
     }

     if (!url.startsWith("http")) {
         url = "https://" + url;
     }

     return url.replace("://www.", "://");
     }

 private String createSiteNameByUrl(String url) {
         String name = url.replace("http://", "");
         name = name.replace("https://", "");
         name = name.replace("www.", "");
         return name;
 }



}
