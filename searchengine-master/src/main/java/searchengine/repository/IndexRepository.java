package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityIndex;
import searchengine.model.EntityPage;

import java.util.HashSet;

@Repository
public interface IndexRepository extends CrudRepository<EntityIndex, Integer> {
    HashSet<EntityIndex> findByPageAndLemma_LemmaIn(EntityPage page, Iterable<String> lemmas);
}
