package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;

import java.util.Optional;

@Repository                                          //Object //тип поля ID
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    //@Query("select p from Page p where p.path=:path and site_id=:site_id")
    //List<Page> findByPathAndSite(@Param("path") String path1, @Param("site_id") int site_id1);
    Optional<PageEntity> findFirstByPathAndSite_id(String path, int site_id);

//     @Query("delete from Page p where p.site_id=:site_id")
     void deleteAllBySite_id(@Param("site_id") int site_id);

     int countBySite_id(@Param("site_id") int site_id);
}
