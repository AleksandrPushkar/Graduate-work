package searchengine.searcher;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SnippetGetterData {
    private final String query;
    private final Set<String> lemmas;
    private final List<SearchPage> pages;
}
