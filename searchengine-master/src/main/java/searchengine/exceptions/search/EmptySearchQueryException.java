package searchengine.exceptions.search;

public class EmptySearchQueryException extends RuntimeException {
    public EmptySearchQueryException() {
        super("Задан пустой поисковый запрос");
    }
}
