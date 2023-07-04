package searchengine.indexer;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.model.*;
import searchengine.repository.*;
import searchengine.services.indexing.IndexingService;

import java.time.LocalDateTime;
import java.util.Map.Entry;

@Component
@RequiredArgsConstructor
public class WorkingWithDatabase {
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private final IndexRepository indexRepo;
    private final LemmaRepository lemmaRepo;
    private final IndexingService indexingService;
    private final LemmaFinder lemmaFinder;

    public EntitySite saveNewSite(Site configSite) {
        EntitySite site = new EntitySite(StatusIndexing.INDEXING, configSite.getUrl(), configSite.getName());
        return siteRepo.save(site);
    }

    public void updateSiteStatusTime(EntitySite site) {
        site.setStatusTime(LocalDateTime.now());
        siteRepo.save(site);
    }

    public void deleteSite(Site configSite) {
        EntitySite entitySite = siteRepo.findByUrl(configSite.getUrl());
        if (entitySite != null) {
            siteRepo.delete(entitySite);
        }
    }

    public boolean synchronizedSavePage(EntityPage page) {
        synchronized (pageRepo) {
            boolean exists = pageRepo.existsBySiteAndPath(page.getSite(), page.getPath());
            if (exists) {
                return false;
            }
            pageRepo.save(page);
            return true;
        }
    }

    public EntityLemma synchronizedSaveLemma(EntityLemma lemma) {
        synchronized (lemmaRepo) {
            EntityLemma foundLemma = lemmaRepo.findBySiteAndLemma(lemma.getSite(), lemma.getLemma());
            if (foundLemma != null) {
                foundLemma.setFrequency(foundLemma.getFrequency() + 1);
                lemma = foundLemma;
            }
            return lemmaRepo.save(lemma);
        }
    }

    public void loopSaveLemmasAndIndexes(Document doc, EntityPage page, EntitySite site) {
        for (Entry<String, Integer> entry : lemmaFinder.getPageLemmas(doc).entrySet()) {
            if (indexingService.isIndexingStopped()) {
                return;
            }
            EntityLemma lemma = new EntityLemma(site, entry.getKey(), 1);
            lemma = synchronizedSaveLemma(lemma);
            int quantity = entry.getValue();
            EntityIndex index = new EntityIndex(page, lemma, quantity);
            indexRepo.save(index);
        }
    }
}
