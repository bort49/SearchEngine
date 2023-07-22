package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name    = "page")
//      ,indexes = {   //this not work for TEXT field/ Just for VARCHAR(255)
//       @Index(name = "path_idx", columnList = "site_id,path", unique = true)
//       })
@Setter
@Getter
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE KEY pathIndex(path(512),site_id)")
    private String path;
    @Column(name = "code", nullable = false)
    private int responseCode;
    //@Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    @Column(length = 16777215, columnDefinition = "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY) //cascade = CascadeType.ALL, !!!! cascade All приведет к тому, что при удалении страницы удалится сайт, а это нам не надо
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "site_id_key"))
    private SiteEntity site;

    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE)
    private List<IndexEntity> index = new ArrayList<>();





}
