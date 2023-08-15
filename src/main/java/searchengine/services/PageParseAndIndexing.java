package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;

import static searchengine.services.IndexingServiceImpl.*;

@Slf4j
public class PageParseAndIndexing extends RecursiveAction {

    private final String pageUrl;
    private String pageUrlForSave;
    private List<PageParseAndIndexing> taskList = new ArrayList<>();

    boolean singlePageKey;

    private final SiteEntity siteEntity;

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private static Set<String> tmpPassedUrlsList;

    private static LemmaFinder lemmaFinder;

    public PageParseAndIndexing(String pageUrl, boolean singlePageKey, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, SiteEntity siteEntity, Set<String> tmpPassedUrlsList) {
        this.pageUrl = pageUrl;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteEntity = siteEntity;
        this.tmpPassedUrlsList = tmpPassedUrlsList;
        this.singlePageKey = singlePageKey;


        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public PageEntity newPageRecord(SiteEntity siteEntity, String path, String content, int responseCode) {
        PageEntity page = new PageEntity();
        page.setPath(path);
        page.setResponseCode(responseCode);
        page.setSite(siteEntity);
        page.setContent(content);
        pageRepository.saveAndFlush(page);
        return page;
    }

/*
    public LemmaEntity saveLemmaRecord(SiteEntity siteEntity, String lemma) {
   //    synchronized (LemmaEntity.class) {
           Optional<LemmaEntity> optionalLemmaEntity = lemmaRepository.findFirstByLemmaAndSite_id(lemma, siteEntity.getId());
           LemmaEntity lemmaEntity;

           if (!optionalLemmaEntity.isPresent()) {
               lemmaEntity = new LemmaEntity();
               lemmaEntity.setLemma(lemma);
               lemmaEntity.setFrequency(1);
               lemmaEntity.setSite(siteEntity);
               lemmaRepository.saveAndFlush(lemmaEntity);
           } else {
               lemmaEntity = optionalLemmaEntity.get();
               lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
               lemmaRepository.saveAndFlush(lemmaEntity);
           }

           return lemmaEntity;
   //    }
    }
*/

    void collectLemmasAndIndexesByPage(String content, PageEntity page) {
//LEMMAS
        //леммы и их количества на странице
        Map<String, Integer> pageLemmaList =  lemmaFinder.collectLemmas(content);
//                    List<LemmaEntity> lemmaEntityList = new CopyOnWriteArrayList<>();

        ArrayList<IndexEntity> IndexEntitySetByPage = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pageLemmaList.entrySet()) {

            LemmaEntity lemma = siteEntity.siteLemmaSet.get(entry.getKey());
            if (lemma == null) {
                lemma = new LemmaEntity();
                lemma.setLemma(entry.getKey());
                lemma.setSite(siteEntity);
                lemma.setFrequency(1);
            }
            else {
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            siteEntity.siteLemmaSet.put(lemma.getLemma(), lemma);


            //        LemmaEntity lemmaEntity = saveLemmaRecord(siteEntity, entry.getKey());

            //index table
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setPage(page);
            indexEntity.setLemma(lemma);
            indexEntity.setRank(entry.getValue());
            siteEntity.siteIndexSet.add(indexEntity);

        }

    }


    @Override
    protected void compute() {

            boolean stopKey = IndexingServiceImpl.stopIndexingRequest;




            if (!stopKey) {

                pageUrlForSave =  pageUrl.substring(siteEntity.getDomainName().length());



                if (pageUrlForSave.trim().isEmpty()) {
                    return;
                }



                Document doc = null;
                try {
                    doc = Jsoup.connect(pageUrl).userAgent(userAgentValue).referrer(referrerValue).timeout(10 * 1000).get();
                } catch (Exception e) {
                    log.info("Can't get page:" + pageUrl);
                    log.info(e.getMessage());
                    if (pageUrl == siteEntity.getUrl()) {
                        siteEntity.setStatus(Status.FAILED);
                    }
                    siteEntity.setLastError(e.getMessage());
                    siteRepository.save(siteEntity);
                    return;
                }


                String content = doc.outerHtml();
                content = Jsoup.parse(content).text();
                Connection.Response response = doc.connection().response();
                int responseCode = response.statusCode();

                if (!pageRepository.findFirstByPathAndSite_id(pageUrlForSave, siteEntity.getId()).isPresent()) {

                    PageEntity newPageRecord = newPageRecord(siteEntity, pageUrlForSave, content, responseCode);

                    tmpPassedUrlsList.remove(pageUrlForSave);


                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteEntity);



//todo  сделать map лемм по индексируемому сайту и сохранять его после полной индексации сайта
                    //  При индексации одной страницы, поднимать текущий набор по сайту, удалять, добавлять количество и сохранять заново


                    collectLemmasAndIndexesByPage(content, newPageRecord);



//                    indexRepository.saveAllAndFlush(IndexEntitySetByPage);


//                        Optional<LemmaEntity> optionalLemmaEntity = lemmaRepository.findFirstByLemmaAndSite_id(entry.getKey(), siteEntity.getId());

//                        if (!optionalLemmaEntity.isPresent()) {
//                            LemmaEntity lemmaEntity = new LemmaEntity();
//                            lemmaEntity.setLemma(entry.getKey());
//                            lemmaEntity.setFrequency(1);
//                            lemmaEntity.setSite(siteEntity);
//                            lemmaRepository.save(lemmaEntity);
//                            lemmaRepository.flush();
//                        }
//                        else {
//                            LemmaEntity lemmaEntity = optionalLemmaEntity.get();
//                            lemmaEntity.setFrequency(lemmaEntity.getFrequency()+1);
//                            lemmaRepository.save(lemmaEntity);
//                        }
//                        lemmaEntityList.add(lemmaEntity);
//                    }

//                    lemmaRepository.flush();





// System.out.println("save lemma list");
//                    lemmaRepository.saveAll(lemmaEntityList);
//                    lemmaRepository.flush();
// System.out.println("saved");

                    pagesCounter++;
                    System.out.print("\rNumber of links (total): " + pagesCounter + ", current: " + pageUrl);

                }
                else return;

                if (singlePageKey) {
                    return;
                }

                //recursive call on all links
                Elements elements = doc.select("a:not([href$='.pdf']):not([href$='.zip']):not([href$='.png']):not([href$='.jpg']):not([href$='#']):not([href^='mailto:'])");

                for (Element element : elements) {
                    String link = element.absUrl("href");
                    if (link.startsWith(siteEntity.getDomainName()) && !link.contains("#") && !link.contains("?")) {
                        pageUrlForSave =  link.substring(siteEntity.getDomainName().length());


                        if (pageUrlForSave.trim().isEmpty()) {
                            continue;
                        }

                          if (tmpPassedUrlsList.add(pageUrlForSave) && !pageRepository.findFirstByPathAndSite_id(pageUrlForSave, siteEntity.getId()).isPresent()) {
                              try {
                                  Thread.sleep(100);

                              } catch (InterruptedException e) {
                                  throw new RuntimeException(e);
                              }

                              PageParseAndIndexing task = new PageParseAndIndexing(link, singlePageKey, siteRepository, pageRepository, lemmaRepository, indexRepository, siteEntity, tmpPassedUrlsList);
                              task.fork();

                              taskList.add(task);

                          }
                    }
                }


                //waiting for tasks to complete
                Collections.reverse(taskList);
                for (PageParseAndIndexing task : taskList) {
                    task.join();
                }
            }


    }
}
