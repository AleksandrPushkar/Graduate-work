package searchengine.searcher;

import lombok.Data;

import java.util.List;

@Data
public class SearchResult {
    private final int totalNumberPages;
    private final List<SearchPage> pages;
}
