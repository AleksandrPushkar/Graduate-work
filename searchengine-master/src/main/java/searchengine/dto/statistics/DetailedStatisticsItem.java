package searchengine.dto.statistics;

import lombok.Data;

import java.util.Date;

@Data
public class DetailedStatisticsItem {
    private final String url;
    private final String name;
    private String status;
    private long statusTime;
    private String error;
    private int pages;
    private int lemmas;

    public DetailedStatisticsItem(String url, String name) {
        this.url = url;
        this.name = name;
        status = "";
        statusTime = new Date().getTime();
        error = "";
    }
}
