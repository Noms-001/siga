package mg.bank.backend.repository.affectation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.affectation.AffectationPlanAction;
import mg.bank.backend.repository.projection.affectation.AffectationPlanActionRow;

public interface AffectationPlanActionRepository
        extends JpaRepository<AffectationPlanAction, Integer> {

    @Query(value = """
            SELECT aff.id_affectation_plan_action AS id,
                   aff.date_affectation            AS dateAffectation,
                   aff.date_desaffectation         AS dateDesaffectation,
                   r.id_role                       AS roleId,
                   r.code                          AS roleCode,
                   r.designation                   AS roleLibelle,
                   u.id_utilisateur                AS utilisateurId,
                   u.nom                           AS utilisateurNom,
                   u.prenom                        AS utilisateurPrenom
            FROM affectation_plan_action aff
            JOIN role r        ON r.id_role = aff.id_role
            JOIN utilisateur u ON u.id_utilisateur = aff.id_utilisateur
            WHERE aff.id_plan_action = :idPlanAction
            ORDER BY aff.date_affectation DESC, aff.id_affectation_plan_action DESC
            """, nativeQuery = true)
    List<AffectationPlanActionRow> findHistorique(@Param("idPlanAction") Integer idPlanAction);
}