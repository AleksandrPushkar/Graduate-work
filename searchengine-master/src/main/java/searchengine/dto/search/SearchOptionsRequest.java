package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchOptionsRequest {
    private final String query;
    private final int offset;
    private final int limit;
    private final String site;
}