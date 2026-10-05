package mg.bank.backend.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "type_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_activite")
    private Integer idTypeActivite;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "description")
    private String description;

    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_desactivation")
    private LocalDateTime dateDesactivation;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "type_activite_service",
            joinColumns = @JoinColumn(name = "id_type_activite"),
            inverseJoinColumns = @JoinColumn(name = "id_service")
    )
    @Builder.Default
    private Set<mg.bank.backend.model.Service> services = new HashSet<>();

}
