package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "page")
public class EntityPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private EntitySite siteId;

    @Column(columnDefinition = "TEXT NOT NULL, Index(path(50))")
    private String path;

    @NonNull
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    @OneToMany(mappedBy = "pageId", cascade = CascadeType.ALL)
    private Set<EntityIndex> indexes;

    public EntityPage(String path) {
        this.path = path;
    }

    public EntityPage(EntitySite siteId, String path) {
        this.siteId = siteId;
        this.path = path;
    }
}
