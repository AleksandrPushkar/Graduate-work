package searchengine.exceptions.indexing;

public class IndexingNotRunningException extends RuntimeException {
    public IndexingNotRunningException() {
        super("Индексация не запущена");
    }
}
