package searchengine.services;

import searchengine.config.Site;
import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse startIndexing();

    IndexingResponse stopIndexing();

    IndexingResponse singlePageIndexing(Site site);
}
