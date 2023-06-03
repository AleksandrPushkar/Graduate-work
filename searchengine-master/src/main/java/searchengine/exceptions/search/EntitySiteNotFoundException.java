package searchengine.exceptions.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EntitySiteNotFoundException extends RuntimeException {
    private final String siteUrl;
}
