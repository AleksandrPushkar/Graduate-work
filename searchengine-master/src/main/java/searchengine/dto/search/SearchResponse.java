package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private final boolean result;
    private final Integer count;
    private final List<PageItem> data;
}
