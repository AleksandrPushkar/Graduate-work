package searchengine.indexer;

import lombok.AllArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;
import searchengine.services.indexing.IndexingService;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

@AllArgsConstructor
public class PagesIndexing extends RecursiveAction {
    private final String rootUrl;
    private final EntitySite site;
    private final JsoupConnect jsoupCon;
    private final LemmaFinder lemmaFinder;
    private final WorkingWithUrl workerUrl;
    private final WorkingWithDatabase workingDB;
    private final IndexingService indexingService;
    private Document docRef;


    @Override
    protected void compute() {
        Elements elements = docRef.select("a[href]");
        docRef = null;
        Set<Document> hashSetForFork = getSetRefsForFork(elements);
        if (indexingService.isIndexingStopped()) {
            return;
        }
        elements = null;
        makeForkJoin(hashSetForFork);
    }

    private Set<Document> getSetRefsForFork(Elements elements) {
        Set<Document> hashSetForFork = new HashSet<>();
        for (Element el : elements) {
            if (indexingService.isIndexingStopped()) {
                return hashSetForFork;
            }
            String absHref = el.attr("abs:href");
            String href = el.attr("href");
            if (href.equals("") || !absHref.contains(rootUrl)
                    || workerUrl.checkForDisqualification(absHref)) {
                continue;
            }
            if (absHref.length() == href.length()) {
                href = workerUrl.cutUrlToPath(href);
            }
            EntityPage page = new EntityPage(site, href);
            Document docHref = jsoupCon.getPageCode(absHref, page, null);
            if (workingDB.synchronizedSavePage(page)) {
                workingDB.updateSiteStatusTime(site);
                if (docHref != null) {
                    hashSetForFork.add(docHref);
                    workingDB.loopSaveLemmasAndIndexes(docHref, page, site);
                }
            }
        }
        return hashSetForFork;
    }

    private void makeForkJoin(Set<Document> hashSetToFork) {
        List<PagesIndexing> subTasks = new LinkedList<>();
        for (Document docHref : hashSetToFork) {
            PagesIndexing task = new PagesIndexing(rootUrl, site, jsoupCon, lemmaFinder,
                    workerUrl, workingDB, indexingService, docHref);
            task.fork();
            subTasks.add(task);
        }
        hashSetToFork = null;
        for (PagesIndexing task : subTasks) {
            task.join();
        }
    }
}