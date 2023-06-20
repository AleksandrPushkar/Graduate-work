package searchengine.searcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchOptionsRequest;
import searchengine.exceptions.common.ConfigSitesNotFoundException;
import searchengine.exceptions.search.*;
import searchengine.indexer.LemmaFinder;
import searchengine.model.EntityIndex;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;
import searchengine.repository.IndexRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.Map.Entry;

@Component
@RequiredArgsConstructor
public class Searcher {
    private static final double PERCENT_MAX_PREVALENCE = 0.2;
    private final SitesList sitesListObj;
    private final LemmaFinder lemmaFinder;
    private final SnippetsGetter snippetsGetter;
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private final IndexRepository indexRepo;

    public SearchResult search(SearchOptionsRequest searchOptionsRequest) {
        String query = searchOptionsRequest.getQuery();
        if (query.isEmpty()) {
            throw new EmptySearchQueryException();
        }
        List<EntitySite> entitySiteList = getEntitySiteList(searchOptionsRequest.getSite());
        List<Lemma> objLemmas = getLemmasWithPages(query, entitySiteList);
        if (objLemmas == null) {
            return null;
        }
        int countAllPages = calculateCountALLPages(entitySiteList);
        excludeLemmasThatOnManyPages(objLemmas, countAllPages);
        Set<EntityPage> pagesWithAllLemmas = findPagesWithAllLemmas(objLemmas);
        if (pagesWithAllLemmas == null) {
            return null;
        }
        List<String> strLemmas = getStrLemmas(objLemmas);
        Map<EntityPage, Float> pagesRelevance = calculateAbsRelevance(strLemmas, pagesWithAllLemmas);
        calculateRelRelevance(pagesRelevance);
        List<SearchPage> sortedPages = sortPagesDescRelevance(pagesRelevance);
        List<SearchPage> sublistSortedPages = getSublist(searchOptionsRequest, sortedPages);
        SnippetGetterData snippetGetterData = new SnippetGetterData(
                searchOptionsRequest.getQuery(), strLemmas, sublistSortedPages);
        snippetsGetter.getSnippets(snippetGetterData);
        return new SearchResult(sortedPages.size(), sublistSortedPages);
    }

    private List<EntitySite> getEntitySiteList(String querySite) {
        List<Site> configSites = sitesListObj.getSites();
        if (configSites.size() == 0) {
            throw new ConfigSitesNotFoundException();
        }
        List<EntitySite> entitySiteList = new ArrayList<>();
        if (querySite != null) {
            entitySiteList.add(getEntitySite(querySite));
            return entitySiteList;
        }
        for (Site configSite : configSites) {
            EntitySite site = siteRepo.findByUrl(configSite.getUrl());
            if (site == null) {
                throw new EntitySiteNotFoundException(configSite.getUrl());
            }
            entitySiteList.add(site);
        }
        return entitySiteList;
    }

    private EntitySite getEntitySite(String querySite) {
        EntitySite site = siteRepo.findByUrl(querySite);
        if (site == null) {
            throw new EntitySiteNotFoundException(querySite);
        }
        return site;
    }

    private List<Lemma> getLemmasWithPages(String query, List<EntitySite> entitySiteList) {
        List<Lemma> objLemmas = new ArrayList<>();
        Set<String> strLemmas = lemmaFinder.collectLemmas(query).keySet();
        if (strLemmas.size() == 0) {
            return null;
        }
        for (String strLemma : strLemmas) {
            List<EntityPage> pages = pageRepo.findBySiteInAndIndexes_Lemma_Lemma(entitySiteList, strLemma);
            if (pages.size() == 0) {
                return null;
            }
            objLemmas.add(new Lemma(strLemma, pages));
        }
        Collections.sort(objLemmas);
        return objLemmas;
    }

    private int calculateCountALLPages(List<EntitySite> entitySiteList) {
        int countAllPages = 0;
        for (EntitySite site : entitySiteList) {
            countAllPages += site.getPages().size();
        }
        return countAllPages;
    }

    private void excludeLemmasThatOnManyPages(List<Lemma> objLemmas, int countAllPages) {
        Iterator<Lemma> lemmaIterator = objLemmas.iterator();
        boolean deleteAllOthers = false;
        while (lemmaIterator.hasNext()) {
            Lemma lemma = lemmaIterator.next();
            if (deleteAllOthers) {
                lemmaIterator.remove();
                continue;
            }
            int numberLemmaPages = lemma.getPages().size();
            double lemmaPrevalence = (double) numberLemmaPages / countAllPages;
            if (lemmaPrevalence > PERCENT_MAX_PREVALENCE) {
                lemmaIterator.remove();
                deleteAllOthers = true;
            }
        }
    }

    private Set<EntityPage> findPagesWithAllLemmas(List<Lemma> objLemmas) {
        Set<EntityPage> pagesWithAllLemmas = null;
        for (Lemma lemma : objLemmas) {
            if (lemma == objLemmas.get(0)) {
                pagesWithAllLemmas = new HashSet<>(lemma.getPages());
                continue;
            }
            pagesWithAllLemmas = findPagesWithFollowingLemma(pagesWithAllLemmas, lemma);
            if (pagesWithAllLemmas.size() == 0) {
                return null;
            }
        }
        return pagesWithAllLemmas;
    }

    private Set<EntityPage> findPagesWithFollowingLemma(Set<EntityPage> pagesWithPreviousLemma, Lemma lemma) {
        Set<EntityPage> pagesWithFollowingLemma = new HashSet<>();
        for (EntityPage page : lemma.getPages()) {
            if (pagesWithPreviousLemma.contains(page)) {
                pagesWithFollowingLemma.add(page);
            }
        }
        return pagesWithFollowingLemma;
    }

    private List<String> getStrLemmas(List<Lemma> objLemmas) {
        return objLemmas.stream().map(Lemma::getLemma).toList();
    }

    private Map<EntityPage, Float> calculateAbsRelevance(List<String> strLemmas,
                                                         Set<EntityPage> pagesWithAllLemmas) {
        Map<EntityPage, Float> pagesRelevance = new HashMap<>();
        for (EntityPage page : pagesWithAllLemmas) {
            Set<EntityIndex> indexes = indexRepo.findByPageAndLemma_LemmaIn(page, strLemmas);
            float relevance = 0.0f;
            for (EntityIndex index : indexes) {
                relevance += index.getRank();
            }
            pagesRelevance.put(page, relevance);
        }
        return pagesRelevance;
    }

    private void calculateRelRelevance(Map<EntityPage, Float> pagesRelevance) {
        float maxAbsRelevance = 0.0f;
        for (float absRelevance : pagesRelevance.values()) {
            if (maxAbsRelevance < absRelevance) {
                maxAbsRelevance = absRelevance;
            }
        }
        for (Entry<EntityPage, Float> entry : pagesRelevance.entrySet()) {
            float absRelevance = entry.getValue();
            entry.setValue(absRelevance / maxAbsRelevance);
        }
    }

    private List<SearchPage> sortPagesDescRelevance(Map<EntityPage, Float> pagesRelevance) {
        List<SearchPage> sortedPages = new ArrayList<>();
        pagesRelevance.forEach((entityPage, relevance) ->
                sortedPages.add(new SearchPage(entityPage, relevance))
        );
        Collections.sort(sortedPages);
        return sortedPages;
    }

    private List<SearchPage> getSublist(SearchOptionsRequest searchOptionsRequest, List<SearchPage> sortedPages) {
        int offset = searchOptionsRequest.getOffset();
        int limit = searchOptionsRequest.getLimit();
        if (sortedPages.size() >= offset + limit) {
            return sortedPages.subList(offset, offset + limit);
        }
        return sortedPages.subList(offset, sortedPages.size());
    }
}
