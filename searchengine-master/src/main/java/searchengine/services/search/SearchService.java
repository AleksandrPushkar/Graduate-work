package searchengine.services.search;

import org.springframework.stereotype.Service;
import searchengine.dto.search.PageItem;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchOptionsRequest;
import searchengine.searcher.SearchPage;

@Service
public interface SearchService {
    SearchResponse search(SearchOptionsRequest options);

    PageItem convert(SearchPage page);
}
