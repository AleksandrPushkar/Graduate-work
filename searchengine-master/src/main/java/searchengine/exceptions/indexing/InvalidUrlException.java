package searchengine.exceptions.indexing;

public class InvalidUrlException extends RuntimeException{
    public InvalidUrlException() {
        super("Недопустимый URL-адрес");
    }
}
