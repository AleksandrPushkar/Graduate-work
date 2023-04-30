package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.EntitySite;

@Repository
public interface SiteRepository extends CrudRepository<EntitySite, Integer> {
    EntitySite findByUrl(String url);
}
