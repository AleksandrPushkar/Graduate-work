package searchengine.searcher;

import lombok.Data;

import java.util.List;

@Data
public class SnippetGetterData {
    private final String query;
    private final List<String> lemmas;
    private final List<SearchPage> pages;
}
