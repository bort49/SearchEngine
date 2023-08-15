package searchengine.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name    = "lemma",
       indexes = {   //this not work for TEXT field/ Just for VARCHAR(255)
       @Index(name = "lemma_SiteId", columnList = "lemma,site_id", unique = true)
       })

public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
//это тоже работает - всесто записи в @Table вверху
//    @Column(columnDefinition = "VARCHAR(255) NOT NULL, UNIQUE KEY lemmaSiteId(lemma,site_id)")
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @ManyToOne(fetch = FetchType.LAZY) //cascade = CascadeType.ALL, !!!! cascade All приведет к тому, что при удалении страницы удалится сайт, а это нам не надо
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "site_id"))
    private SiteEntity site;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.REMOVE)
    private List<IndexEntity> index = new ArrayList<>();

}
