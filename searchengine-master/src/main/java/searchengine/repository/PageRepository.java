package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;

@Repository
public interface PageRepository extends CrudRepository<EntityPage, Integer> {
    int countAllBySiteId(EntitySite site);
    Iterable<EntityPage> findAllByPath(String path);
}
