package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

}
