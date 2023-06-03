package searchengine.workersservices.searcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OptionsSearch {
    private final String query;
    private final int offset;
    private final int limit;
    private final String site;
}
