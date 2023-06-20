package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityIndex;
import searchengine.model.EntityPage;

import java.util.Set;

@Repository
public interface IndexRepository extends CrudRepository<EntityIndex, Integer> {
    Set<EntityIndex> findByPageAndLemma_LemmaIn(EntityPage page, Iterable<String> lemmas);
}
