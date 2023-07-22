package searchengine.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name    = "lemma")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @ManyToOne(fetch = FetchType.LAZY) //cascade = CascadeType.ALL, !!!! cascade All приведет к тому, что при удалении страницы удалится сайт, а это нам не надо
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "site_id_lemma"))
    private SiteEntity site;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.REMOVE)
    private List<IndexEntity> index = new ArrayList<>();

}
