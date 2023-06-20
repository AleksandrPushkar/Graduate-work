package searchengine.searcher;

import lombok.Data;
import searchengine.model.EntityPage;

@Data
public class SearchPage implements Comparable<SearchPage> {
    private final EntityPage entityPage;
    private final float relevance;
    private String title;
    private String snippet;

    @Override
    public int compareTo(SearchPage o) {
        int result = Double.compare(o.relevance, this.relevance);
        if (result == 0) {
            result = this.entityPage.getPath().compareTo(o.entityPage.getPath());
        }
        if (result == 0) {
            String urlSiteThis = this.entityPage.getSite().getUrl();
            String urlSiteO = o.entityPage.getSite().getUrl();
            result = urlSiteThis.compareTo(urlSiteO);
        }
        return result;
    }
}
