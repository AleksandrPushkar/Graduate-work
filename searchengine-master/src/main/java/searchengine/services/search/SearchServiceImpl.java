package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.PageItem;
import searchengine.dto.search.SearchResponse;
import searchengine.model.EntityIndex;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;
import searchengine.repository.IndexRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.workersservices.searcher.MinerSnippet;
import searchengine.workersservices.searcher.OptionsSearch;
import searchengine.workersservices.searcher.SearchPage;
import searchengine.workersservices.searcher.Searcher;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SitesList sitesListObj;
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private final IndexRepository indexRepo;

    @Override
    public SearchResponse search(OptionsSearch options) {
        long start = System.currentTimeMillis();
        Searcher searcher = new Searcher(
                options, this, sitesListObj.getSites());

        List<SearchPage> pages = searcher.search();
        if (pages == null) {
            return new SearchResponse(true, 0, new ArrayList<>(), null);
        }

        MinerSnippet minerSnippet = new MinerSnippet(options.getQuery(),
                pages, searcher.getLemmasStr());

        minerSnippet.mine();
        if (pages.size() == 0)
            return new SearchResponse(true, 0, new ArrayList<>(), null);

        List<PageItem> dtoItems = pages.stream().map(this::convert).toList();
        long finish = System.currentTimeMillis();
        System.out.println("Время выполнения: " + (finish - start));
        return new SearchResponse(true,
                searcher.getTotalNumberPages(), dtoItems, null);
    }

    @Override
    public PageItem convert(SearchPage page) {
        String siteUrl = page.getEntityPage().getSite().getUrl();
        if (siteUrl.charAt(siteUrl.length() - 1) == '/')
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);

        return new PageItem(
                siteUrl,
                page.getEntityPage().getSite().getName(),
                page.getEntityPage().getPath(),
                page.getTitle(),
                page.getSnippet(),
                page.getRelevance());
    }

    @Override
    public EntitySite findSiteByUrl(String url) {
        return siteRepo.findByUrl(url);
    }

    @Override
    public List<EntityPage> findPagesByLemmaAndSites(
            String lemma, ArrayList<EntitySite> sites) {

        return pageRepo.findByIndexes_Lemma_LemmaAndIndexes_Lemma_SiteIn(
                lemma, sites);
    }

    @Override
    public HashSet<EntityIndex> findIndexesByPageAndLemmas(
            EntityPage page, HashSet<String> lemmas) {

        return indexRepo.findByPageAndLemma_LemmaIn(page, lemmas);
    }
}
