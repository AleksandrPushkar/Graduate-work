package searchengine.workersservices.searcher;

import lombok.RequiredArgsConstructor;
import searchengine.config.Site;
import searchengine.exceptions.search.*;
import searchengine.services.search.SearchService;
import searchengine.workersservices.indexer.LemmaFinder;
import searchengine.model.EntityIndex;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;

import java.util.*;
import java.util.Map.Entry;

@RequiredArgsConstructor
public class Searcher {
    private static final double PERCENT_MAX_PREVALENCE = 0.2;
    private final OptionsSearch options;
    private final SearchService searchService;
    private final List<Site> sitesConfig;
    private ArrayList<EntitySite> sitesDB;
    private ArrayList<Lemma> lemmas;
    private HashSet<EntityPage> pagesWithAllLemmas;
    private HashMap<EntityPage, Float> pagesRelevance;
    private List<SearchPage> sortedPages;

    public List<SearchPage> search() {
        if (options.getQuery().isEmpty()) {
            throw new EmptySearchQueryException();
        }

        getListEntitiesSite(options.getSite());
        getPagesLemmas();
        if (lemmas == null)
            return null;

        excludeLemmasThatOnManyPages();
        findPagesWithAllLemmas();
        if (pagesWithAllLemmas == null) {
            return null;
        }

        calculateAbsRelevance();
        calculateRelRelevance();
        sortPagesDescRelevance();
        return getSubList();
    }

    private void getListEntitiesSite(String siteQuery) {
        if (sitesConfig.size() == 0)
            throw new ConfigSitesNotFoundException();

        sitesDB = new ArrayList<>();
        if (siteQuery != null) {
            getEntitySite(siteQuery);
            return;
        }

        for (Site site : sitesConfig) {
            EntitySite entitySite = searchService.findSiteByUrl(site.getUrl());
            if (entitySite == null)
                throw new EntitySiteNotFoundException(site.getUrl());

            sitesDB.add(entitySite);
        }
    }

    private void getPagesLemmas() {
        LemmaFinder lemmaFinder = new LemmaFinder();
        if (lemmaFinder.getLuceneMorphology() == null) {
            throw new LemmasFinderNotReadyWorkException();
        }

        lemmas = new ArrayList<>();
        String query = options.getQuery();
        for (String lemma : lemmaFinder.collectLemmas(query).keySet()) {
            List<EntityPage> pages = searchService
                    .findPagesByLemmaAndSites(lemma, sitesDB);

            if (pages.size() == 0) {
                lemmas = null;
                return;
            }

            lemmas.add(new Lemma(lemma, pages));
        }

        Collections.sort(lemmas);
    }

    private void excludeLemmasThatOnManyPages() {
        int countAllPages = calculateCountALLPages();
        Iterator<Lemma> lemmaIterator = lemmas.iterator();
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

    private void findPagesWithAllLemmas() {
        for (Lemma lemma : lemmas) {
            if (lemma == lemmas.get(0)) {
                pagesWithAllLemmas = new HashSet<>(lemma.getPages());
                continue;
            }

            HashSet<EntityPage> pagesWithFollowingLemma = new HashSet<>();
            for (EntityPage page : lemma.getPages()) {
                if (pagesWithAllLemmas.contains(page))
                    pagesWithFollowingLemma.add(page);
            }

            if (pagesWithFollowingLemma.size() == 0) {
                pagesWithAllLemmas = null;
                return;
            }

            pagesWithAllLemmas = pagesWithFollowingLemma;
        }
    }

    private void calculateAbsRelevance() {
        HashSet<String> lemmasStr = getLemmasStr();
        pagesRelevance = new HashMap<>();
        for (EntityPage page : pagesWithAllLemmas) {
            HashSet<EntityIndex> indexes = searchService
                    .findIndexesByPageAndLemmas(page, lemmasStr);

            if (indexes.isEmpty()) {
                throw new NotFoundIndexesException();
            }

            float relevance = 0.0f;
            for (EntityIndex index : indexes) {
                relevance += index.getRank();
            }

            pagesRelevance.put(page, relevance);
        }
    }

    private void calculateRelRelevance() {
        float maxAbsRelevance = 0.0f;
        for (float absRelevance : pagesRelevance.values()) {
            if (maxAbsRelevance < absRelevance)
                maxAbsRelevance = absRelevance;
        }

        Set<Entry<EntityPage, Float>> setEntries = pagesRelevance.entrySet();
        for (Entry<EntityPage, Float> entry : setEntries) {
            float absRelevance = entry.getValue();
            float relRelevance = absRelevance / maxAbsRelevance;
            entry.setValue(relRelevance);
        }
    }

    private void sortPagesDescRelevance() {
        sortedPages = new ArrayList<>();
        pagesRelevance.forEach((entityPage, relevance) -> {
            SearchPage objPage = new SearchPage(entityPage, relevance);
            sortedPages.add(objPage);
        });

        Collections.sort(sortedPages);
    }

    private List<SearchPage> getSubList() {
        int offset = options.getOffset();
        int limit = options.getLimit();
        if (sortedPages.size() >= offset + limit)
            return sortedPages.subList(offset, offset + limit);

        return sortedPages.subList(offset, sortedPages.size());
    }

    private void getEntitySite(String siteQuery) {
        EntitySite entitySite = searchService.findSiteByUrl(siteQuery);
        if (entitySite == null)
            throw new EntitySiteNotFoundException(siteQuery);

        sitesDB.add(entitySite);
    }

    private int calculateCountALLPages() {
        int countAllPages = 0;
        for (EntitySite site : sitesDB) {
            countAllPages += site.getPages().size();
        }

        return countAllPages;
    }

    public HashSet<String> getLemmasStr() {
        if (lemmas == null)
            return null;

        return new HashSet<>(
                lemmas.stream().map(Lemma::getLemma).toList());
    }

    public int getTotalNumberPages() {
        return sortedPages.size();
    }
}
