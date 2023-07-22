package searchengine.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "site"
     ,indexes = {
      @Index(name = "url", columnList = "url", unique = true),
      @Index(name = "name", columnList = "name", unique = true)
      })
@Setter
@Getter
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING','INDEXED','FAILED')", nullable = false)
    private Status status;
    @Column(name = "status_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    //05072023
    @OneToMany(mappedBy = "site", cascade = CascadeType.REMOVE) //, cascade = CascadeType.ALL - глючит когда много данных //cascade All приведет к тому, что при сайта удалятся его страницы
    private Set<PageEntity> pageSet;

    @OneToMany(mappedBy = "site", cascade = CascadeType.REMOVE) //, cascade = CascadeType.ALL - глючит когда много данных //cascade All приведет к тому, что при сайта удалятся его страницы
    private Set<LemmaEntity> lemmaSet;


    @Transient
    private String domainName;

}
