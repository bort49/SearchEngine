package searchengine.services;

import searchengine.config.Site;
import searchengine.dto.indexing.IndexingResponse;

import java.util.List;

public interface IndexingService {
    IndexingResponse startIndexing(List<Site> sitesList);

    IndexingResponse stopIndexing();

 //   IndexingResponse singlePageIndexing(Site site);
}
