package searchengine.exceptions.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PageCodeNotReceivedException extends RuntimeException {
    private final int code;
}
