package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "site")
public class EntitySite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL")
    private StatusIndexing status;

    @Column(name = "status_time", columnDefinition = "DATETIME NOT NULL")
    private LocalDateTime statusTime;

    @Column(name = "last_Error", columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private Set<EntityPage> firstDeletePages;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private Set<EntityLemma> secondDeleteLemmas;

    public EntitySite(StatusIndexing status, String url, String name) {
        this.status = status;
        this.url = url;
        this.name = name;
        statusTime = LocalDateTime.now();
    }

    public Set<EntityPage> getPages() {
        return firstDeletePages;
    }
}
