package searchengine.exceptions.indexing;

public class SiteNotFoundException extends RuntimeException {
    public SiteNotFoundException() {
        super("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }
}
