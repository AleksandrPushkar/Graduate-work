package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.PageItem;
import searchengine.dto.search.SearchOptionsRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.searcher.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final Searcher searcher;

    @Override
    public SearchResponse search(SearchOptionsRequest searchOptionsRequest) {
        SearchResult searchResult = searcher.search(searchOptionsRequest);
        if (searchResult == null) {
            return new SearchResponse(true, 0, new ArrayList<>());
        }
        List<PageItem> dtoItems = searchResult.getPages().stream().map(this::convert).toList();
        return new SearchResponse(true, searchResult.getTotalNumberPages(), dtoItems);
    }

    @Override
    public PageItem convert(SearchPage page) {
        String siteUrl = page.getEntityPage().getSite().getUrl();
        if (siteUrl.charAt(siteUrl.length() - 1) == '/') {
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
        }
        return new PageItem(
                siteUrl,
                page.getEntityPage().getSite().getName(),
                page.getEntityPage().getPath(),
                page.getTitle(),
                page.getSnippet(),
                page.getRelevance());
    }
}
