package searchengine.workersservices.indexer;

import lombok.AllArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.EntityIndex;
import searchengine.model.EntityLemma;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;
import searchengine.services.indexing.IndexingService;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.RecursiveAction;

@AllArgsConstructor
public class PagesIndexing extends RecursiveAction {
    private final String rootUrl;
    private final EntitySite site;
    private final JsoupConnect jsoupCon;
    private final LemmaFinder lemmaFinder;
    private final IndexingService indexingService;
    private Document docRef;

    @Override
    protected void compute() {
        Elements elements = docRef.select("a[href]");
        docRef = null;
        HashSet<Document> hashSetForFork = getSetRefsForFork(elements);
        elements = null;
        if (indexingService.isIndexingStopped())
            return;

        makeForkJoin(hashSetForFork);
    }

    private HashSet<Document> getSetRefsForFork(Elements elements) {
        HashSet<Document> hashSetForFork = new HashSet<>();
        for (Element el : elements) {
            if (indexingService.isIndexingStopped())
                return hashSetForFork;

            String absHref = el.attr("abs:href");
            String href = el.attr("href");

            if (href.equals("") || !absHref.contains(rootUrl) ||
                    WorkUrl.checkForDisqualification(absHref))
                continue;

            if (absHref.length() == href.length())
                href = WorkUrl.cutUrlToPath(href);

            EntityPage page = new EntityPage(site, href);
            Document docHref = jsoupCon.getPageCode(absHref, page, null);
            if (indexingService.synchronizedPageSave(page)) {
                indexingService.updateSiteStatusTime(site);
                if (docHref != null) {
                    hashSetForFork.add(docHref);
                    loopAddingLemmasAndIndexesInDB(docHref, page);
                }
            }
        }

        return hashSetForFork;
    }

    private void loopAddingLemmasAndIndexesInDB(Document doc, EntityPage page) {
        Map<String, Integer> lemmasPage = lemmaFinder.getLemmasPages(doc);
        for (String strLemma : lemmasPage.keySet()) {
            if (indexingService.isIndexingStopped())
                return;

            EntityLemma lemma = new EntityLemma(site, strLemma, 1);
            lemma = indexingService.synchronizedLemmaSave(lemma);
            int quantity = lemmasPage.get(strLemma);
            EntityIndex index = new EntityIndex(page, lemma, quantity);
            indexingService.saveIndex(index);
        }
    }

    private void makeForkJoin(HashSet<Document> hashSetToFork) {
        LinkedList<PagesIndexing> subTasks = new LinkedList<>();
        for (Document docHref : hashSetToFork) {
            PagesIndexing task = new PagesIndexing(rootUrl,
                    site, jsoupCon, lemmaFinder, indexingService, docHref);

            task.fork();
            subTasks.add(task);
        }

        hashSetToFork = null;
        for (PagesIndexing task : subTasks) {
            task.join();
        }
    }
}