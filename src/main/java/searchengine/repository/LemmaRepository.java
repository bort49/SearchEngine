package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface LemmaRepository  extends JpaRepository<LemmaEntity, Integer> {

    Optional<LemmaEntity> findFirstByLemmaAndSite_id(String lemma, int site_id);

    //     @Query("delete from Lemma l where l.site_id=:site_id")
    void deleteAllBySite_id(@Param("site_id") int site_id);

    int countBySite_id(@Param("site_id") int site_id);

}
