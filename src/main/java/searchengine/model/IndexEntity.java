package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "search_index")
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "lemma_rank", nullable = false)
    private float rank;// FLOAT NOT NULL — количество данной леммы для данной страницы.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false, foreignKey = @ForeignKey(name = "page_id_key"))
    private PageEntity page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false, foreignKey = @ForeignKey(name = "lemma_id_key"))
    private LemmaEntity lemma;


}
