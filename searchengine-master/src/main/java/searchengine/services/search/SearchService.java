package searchengine.services.search;

import org.springframework.stereotype.Service;
import searchengine.dto.search.PageItem;
import searchengine.dto.search.SearchResponse;
import searchengine.model.EntityIndex;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;
import searchengine.workersservices.searcher.OptionsSearch;
import searchengine.workersservices.searcher.SearchPage;

import java.util.*;

@Service
public interface SearchService {
    SearchResponse search(OptionsSearch options);
    PageItem convert(SearchPage page);
    EntitySite findSiteByUrl(String url);
    List<EntityPage> findPagesByLemmaAndSites(
            String lemma, ArrayList<EntitySite> sites);

    HashSet<EntityIndex> findIndexesByPageAndLemmas(
            EntityPage page, HashSet<String> lemmas);
}
