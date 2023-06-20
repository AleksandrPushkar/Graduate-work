package searchengine.searcher;

import lombok.Data;
import searchengine.model.EntityPage;

import java.util.List;

@Data
public class Lemma implements Comparable<Lemma>{
    private final String lemma;
    private final List<EntityPage> pages;

    @Override
    public int compareTo(Lemma o) {
        return this.pages.size() - o.pages.size();
    }
}
