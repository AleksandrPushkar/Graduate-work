package searchengine.exceptions.indexing;

public class IndexingAlreadyRunningException extends RuntimeException {
    public IndexingAlreadyRunningException() {
        super("Индексация уже запущена");
    }
}
