package mg.bank.backend.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Liaison plan d'action ↔ origine.
 *
 * Cle composite (id_plan_action, id_origine) portee par {@link Cle}.
 * est_principale est hors cle : la contrainte en base garantit au plus
 * une principale par plan, mais n'impose pas qu'il y en ait une.
 */
@Entity
@Table(name = "plan_action_origine")
@IdClass(PlanActionOrigine.Cle.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanActionOrigine {

    @Id
    @Column(name = "id_plan_action")
    private Integer idPlanAction;

    @Id
    @Column(name = "id_origine")
    private Integer idOrigine;

    @Column(name = "est_principale")
    private Boolean estPrincipale;

    public static class Cle implements Serializable {
        private Integer idPlanAction;
        private Integer idOrigine;

        public Cle() { }

        public Cle(Integer idPlanAction, Integer idOrigine) {
            this.idPlanAction = idPlanAction;
            this.idOrigine = idOrigine;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cle cle)) return false;
            return Objects.equals(idPlanAction, cle.idPlanAction)
                    && Objects.equals(idOrigine, cle.idOrigine);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idPlanAction, idOrigine);
        }
    }
}