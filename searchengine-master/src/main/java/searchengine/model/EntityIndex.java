package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`index`")
public class EntityIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private EntityPage page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private EntityLemma lemma;

    @Column(name = "`rank`", nullable = false)
    private float rank;

    public EntityIndex(EntityPage page, EntityLemma lemma, float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }
}
