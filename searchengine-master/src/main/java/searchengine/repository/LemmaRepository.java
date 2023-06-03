package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityLemma;
import searchengine.model.EntitySite;

@Repository
public interface LemmaRepository extends CrudRepository<EntityLemma, Integer> {
    int countAllBySite(EntitySite site);
    EntityLemma findBySiteAndLemma(EntitySite site, String lemma);
}
