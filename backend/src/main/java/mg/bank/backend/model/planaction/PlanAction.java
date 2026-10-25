package mg.bank.backend.model.planaction;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plan d'action — module independant de l'activite.
 *
 * Le statut courant n'est PAS porte par cette table : il est lu depuis
 * la derniere ligne de avancement_plan_action. La creation insere une
 * premiere ligne avec le statut BROUILLON.
 */
@Entity
@Table(name = "plan_action")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan_action")
    private Integer idPlanAction;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "date_debut_prevue", nullable = false)
    private LocalDate dateDebutPrevue;

    @Column(name = "date_fin_prevue", nullable = false)
    private LocalDate dateFinPrevue;

    @Column(name = "date_debut_reelle")
    private LocalDate dateDebutReelle;

    @Column(name = "date_fin_reelle")
    private LocalDate dateFinReelle;
}