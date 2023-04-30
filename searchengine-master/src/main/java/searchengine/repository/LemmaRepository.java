package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityLemma;
import searchengine.model.EntitySite;

@Repository
public interface LemmaRepository extends CrudRepository<EntityLemma, Integer> {
    int countAllBySiteId(EntitySite site);
    Iterable<EntityLemma> findAllByLemma(String lemma);
}
