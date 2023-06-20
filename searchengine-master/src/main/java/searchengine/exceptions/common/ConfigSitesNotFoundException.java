package searchengine.exceptions.common;

public class ConfigSitesNotFoundException extends RuntimeException{
    public ConfigSitesNotFoundException() {
        super("В конфигурационном файле не указаны сайты для индексации и поиска");
    }
}
