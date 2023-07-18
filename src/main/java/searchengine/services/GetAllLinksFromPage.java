package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;

import static searchengine.services.IndexingServiceImpl.*;

@Slf4j
public class GetAllLinksFromPage extends RecursiveAction {

    private final String pageUrl;
    private String pageUrlForSave;
    private List<GetAllLinksFromPage> taskList = new ArrayList<>();

    private final SiteEntity siteEntity;

    private final PageRepository pageRepository;

    private static Set<String> tmpPassedUrlsList;


    public GetAllLinksFromPage(String pageUrl, PageRepository pageRepository, SiteEntity siteEntity, Set<String> tmpPassedUrlsList) {
        this.pageUrl = pageUrl;
        this.pageRepository = pageRepository;
        this.siteEntity = siteEntity;
        this.tmpPassedUrlsList = tmpPassedUrlsList;

    }

    public void newPageRecord(SiteEntity siteEntity, String path, String content, int responseCode) {
        PageEntity page = new PageEntity();
        page.setPath(path);
        page.setResponseCode(responseCode);
        page.setSite(siteEntity);
        page.setContent(content);
        pageRepository.save(page);
        pageRepository.flush();
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
                    return;
                }
                //           Connection.Response response = Jsoup.connect(pageUrl).userAgent(userAgentValue).timeout(10 * 1000).execute();
     //           Document doc = response.parse();

                String content = doc.outerHtml();
                content = Jsoup.parse(content).text();
                Connection.Response response = doc.connection().response();
                int responseCode = response.statusCode();
                Elements elements = doc.select("a:not([href$='.pdf']):not([href$='.zip']):not([href$='.png']):not([href$='.jpg']):not([href$='#']):not([href^='mailto:'])");



                if (!pageRepository.findFirstByPathAndSite_id(pageUrlForSave, siteEntity.getId()).isPresent()) {
                    newPageRecord(siteEntity, pageUrlForSave, content, responseCode);
                    tmpPassedUrlsList.remove(pageUrlForSave);
                }
                else return;




                for (Element element : elements) {
                    String link = element.absUrl("href");
                    if (link.startsWith(siteEntity.getDomainName()) && !link.contains("#") && !link.contains("?")) {
                        pageUrlForSave =  link.substring(siteEntity.getDomainName().length());


                        if (pageUrlForSave.trim().isEmpty()) {
                            continue;
                        }

                          if (tmpPassedUrlsList.add(pageUrlForSave) && !pageRepository.findFirstByPathAndSite_id(pageUrlForSave, siteEntity.getId()).isPresent()) {
//                        if (!pageRepository.findFirstByPathAndSite_id(pageUrlForSave, siteEntity.getId()).isPresent()) {

   //                         System.out.println("not found:  " + pageUrlForSave+"-"+siteEntity.getId());
                              try {
                                  Thread.sleep(150);

                              } catch (InterruptedException e) {
                                  throw new RuntimeException(e);
                              }

                              pagesCounter++;
                              System.out.print("\rNumber of links (total): " + pagesCounter + ", current: " + link);

                              GetAllLinksFromPage task = new GetAllLinksFromPage(link,  pageRepository, siteEntity, tmpPassedUrlsList);
                              task.fork();

                              taskList.add(task);

                          }
                    }
                }


                //waiting for tasks to complete
                Collections.reverse(taskList);
                for (GetAllLinksFromPage task : taskList) {
                    task.join();
                }
            }


    }
}