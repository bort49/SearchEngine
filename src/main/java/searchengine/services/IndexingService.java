package searchengine.services;

import searchengine.config.Site;
import searchengine.dto.indexing.IndexingResponse;

import java.util.List;

public interface IndexingService {
    IndexingResponse startIndexing();

    IndexingResponse pageIndexing(String url);

    IndexingResponse stopIndexing();

 //   IndexingResponse singlePageIndexing(Site site);
}
