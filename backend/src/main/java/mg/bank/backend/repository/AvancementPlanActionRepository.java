package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.AvancementPlanAction;
import mg.bank.backend.repository.projection.AvancementPlanActionRow;

public interface AvancementPlanActionRepository
                extends JpaRepository<AvancementPlanAction, Integer> {

        @Query(value = """
                        SELECT av.id_avancement_plan_action AS id,
                               av.valeur_pourcentage        AS valeurPourcentage,
                               av.commentaire               AS commentaire,
                               av.date_changement           AS dateChangement,
                               s.id_statut                  AS statutId,
                               s.code                       AS statutCode,
                               s.libelle                    AS statutLibelle,
                               u.id_utilisateur             AS utilisateurId,
                               u.nom                        AS utilisateurNom,
                               u.prenom                     AS utilisateurPrenom
                        FROM avancement_plan_action av
                        LEFT JOIN statut s      ON s.id_statut = av.id_statut
                        LEFT JOIN utilisateur u ON u.id_utilisateur = av.id_utilisateur
                        WHERE av.id_plan_action = :idPlanAction
                        ORDER BY av.date_changement DESC, av.id_avancement_plan_action DESC
                        """, nativeQuery = true)
        List<AvancementPlanActionRow> findHistorique(@Param("idPlanAction") Integer idPlanAction);

        /**
         * Derniere ligne d'avancement : statut et valeur courants.
         */
        @Query(value = """
                        SELECT av.id_avancement_plan_action AS id,
                               av.valeur_pourcentage        AS valeurPourcentage,
                               av.commentaire               AS commentaire,
                               av.date_changement           AS dateChangement,
                               s.id_statut                  AS statutId,
                               s.code                       AS statutCode,
                               s.libelle                    AS statutLibelle,
                               u.id_utilisateur             AS utilisateurId,
                               u.nom                        AS utilisateurNom,
                               u.prenom                     AS utilisateurPrenom
                        FROM avancement_plan_action av
                        LEFT JOIN statut s      ON s.id_statut = av.id_statut
                        LEFT JOIN utilisateur u ON u.id_utilisateur = av.id_utilisateur
                        WHERE av.id_plan_action = :idPlanAction
                        ORDER BY av.date_changement DESC, av.id_avancement_plan_action DESC
                        LIMIT 1
                        """, nativeQuery = true)
        Optional<AvancementPlanActionRow> findDernier(@Param("idPlanAction") Integer idPlanAction);
}