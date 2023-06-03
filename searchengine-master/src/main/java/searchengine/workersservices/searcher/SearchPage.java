package searchengine.workersservices.searcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import searchengine.model.EntityPage;

@Getter
@Setter
@RequiredArgsConstructor
public class SearchPage implements Comparable<SearchPage> {
    private final EntityPage entityPage;
    private final float relevance;
    private String title;
    private String snippet;

    @Override
    public int compareTo(SearchPage o) {
        int result = Double.compare(o.relevance, this.relevance);
        if (result != 0)
            return result;

        result = this.entityPage.getPath().compareTo(
                o.entityPage.getPath());
        if (result != 0)
            return result;

        String urlSiteThis = this.entityPage.getSite().getUrl();
        String urlSiteO = o.entityPage.getSite().getUrl();
        return urlSiteThis.compareTo(urlSiteO);
    }
}
