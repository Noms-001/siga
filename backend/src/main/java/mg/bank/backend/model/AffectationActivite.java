package mg.bank.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Affectation d'une activite a un utilisateur, portant un role.
 *
 * <p>La desaffectation n'est pas un DELETE : elle positionne
 * {@code date_desaffectation}. L'historique est ainsi conserve, et la regle
 * « un seul actif par (activite, role) » est portee par un index unique
 * partiel (voir la migration).</p>
 */
@Entity
@Table(name = "affectation_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_affectation_activite")
    private Integer idAffectationActivite;

    @Column(name = "date_affectation", nullable = false)
    private LocalDateTime dateAffectation;

    @Column(name = "date_desaffectation")
    private LocalDateTime dateDesaffectation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_activite", nullable = false)
    private Activite activite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;
}