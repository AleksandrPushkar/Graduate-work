package searchengine.workersservices.searcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import searchengine.model.EntityPage;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Lemma implements Comparable<Lemma>{
    private final String lemma;
    private final List<EntityPage> pages; //Страницы на которых встречается лемма

    @Override
    public int compareTo(Lemma o) {
        return this.pages.size() - o.pages.size();
    }
}
