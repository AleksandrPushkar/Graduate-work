package searchengine.exceptions.search;

public class EntitySiteNotFoundException extends RuntimeException {
    public EntitySiteNotFoundException(String siteUrl) {
        super("Сайт " + siteUrl + " не проиндексирован");
    }
}
