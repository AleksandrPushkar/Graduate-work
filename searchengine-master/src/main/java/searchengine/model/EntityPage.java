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
@Table(name = "page", uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "path"}))
public class EntityPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private EntitySite site;

    @Column(columnDefinition = "TEXT NOT NULL")
    private String path;

    @NonNull
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private Set<EntityIndex> indexes;

    public EntityPage(String path) {
        this.path = path;
    }

    public EntityPage(EntitySite site, String path) {
        this.site = site;
        this.path = path;
    }
}
