package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity

@Table(name = "lemma")
public class EntityLemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private EntitySite site;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL, Index(lemma, site_id)")
    private String lemma;

    @NonNull
    private int frequency;

    public EntityLemma(EntitySite site, String lemma, int frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
