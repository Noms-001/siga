package mg.bank.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"procedure\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_procedure")
    private Integer idProcedure;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "description")
    private String description;

    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "date_desactivation")
    private LocalDateTime dateDesactivation;

    @OneToMany(
            mappedBy = "procedure",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @SQLRestriction("actif = true")
    @OrderBy("niveau ASC")
    @Builder.Default
    private List<EtapeValidation> etapesValidation = new ArrayList<>();
}