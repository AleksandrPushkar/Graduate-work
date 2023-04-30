package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.EntityIndex;

@Repository
public interface IndexRepository extends CrudRepository<EntityIndex, Integer> {
}
