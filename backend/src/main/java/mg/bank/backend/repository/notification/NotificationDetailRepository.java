package mg.bank.backend.repository.notification;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.notification.Notification;
import mg.bank.backend.repository.projection.notification.ActiviteContexteRow;
import mg.bank.backend.repository.projection.notification.IndicateurContexteRow;
import mg.bank.backend.repository.projection.notification.PlanActionContexteRow;
import mg.bank.backend.repository.projection.notification.SousActiviteContexteRow;

public interface NotificationDetailRepository extends JpaRepository<Notification, Integer> {

    @Query(value = """
        SELECT act.id_activite                     AS id,
               act.code                            AS code,
               act.designation                     AS designation,
               act.date_fin_prevue                 AS dateFinPrevue,
               sta.code                            AS statutCode,
               sta.libelle                         AS statutLibelle,
               CAST(COALESCE(av.avancement, 0) AS DOUBLE PRECISION) AS avancement
        FROM activite act
        LEFT JOIN LATERAL (
            SELECT h.id_statut
            FROM historique_activite h
            WHERE h.id_activite = act.id_activite
            ORDER BY h.date_changement DESC, h.id_historique_activite DESC
            LIMIT 1
        ) hist ON TRUE
        LEFT JOIN statut sta ON sta.id_statut = hist.id_statut
        LEFT JOIN LATERAL (
            SELECT AVG(dernier.valeur_pourcentage) AS avancement
            FROM sous_activite sa
            JOIN LATERAL (
                SELECT av2.valeur_pourcentage
                FROM avancement_sous_activite av2
                WHERE av2.id_sous_activite = sa.id_sous_activite
                ORDER BY av2.date_changement DESC, av2.id_historique_sous_activite DESC
                LIMIT 1
            ) dernier ON TRUE
            WHERE sa.id_activite = act.id_activite
        ) av ON TRUE
        WHERE act.id_activite = :id
        """, nativeQuery = true)
    Optional<ActiviteContexteRow> findActiviteContexte(@Param("id") Integer idActivite);

    @Query(value = """
        SELECT sa.id_sous_activite                 AS id,
               sa.id_activite                      AS idActivite,
               sa.code                             AS code,
               sa.designation                      AS designation,
               sa.date_fin_prevue                  AS dateFinPrevue,
               sta.code                            AS statutCode,
               sta.libelle                         AS statutLibelle,
               CAST(COALESCE(av.valeur_pourcentage, 0) AS DOUBLE PRECISION) AS avancement,
               u_resp.prenom                       AS responsablePrenom,
               u_resp.nom                          AS responsableNom
        FROM sous_activite sa
        LEFT JOIN LATERAL (
            SELECT av2.valeur_pourcentage, av2.id_statut
            FROM avancement_sous_activite av2
            WHERE av2.id_sous_activite = sa.id_sous_activite
            ORDER BY av2.date_changement DESC, av2.id_historique_sous_activite DESC
            LIMIT 1
        ) av ON TRUE
        LEFT JOIN statut sta ON sta.id_statut = av.id_statut
        LEFT JOIN LATERAL (
            SELECT aff.id_utilisateur
            FROM affectation_sous_activite aff
            JOIN role r ON r.id_role = aff.id_role AND r.code = 'RESPONSABLE'
            WHERE aff.id_sous_activite = sa.id_sous_activite
              AND aff.date_desaffectation IS NULL
            ORDER BY aff.date_affectation DESC, aff.id_affectation_sous_activite DESC
            LIMIT 1
        ) aff_resp ON TRUE
        LEFT JOIN utilisateur u_resp ON u_resp.id_utilisateur = aff_resp.id_utilisateur
        WHERE sa.id_sous_activite = :id
        """, nativeQuery = true)
    Optional<SousActiviteContexteRow> findSousActiviteContexte(@Param("id") Integer idSousActivite);

    @Query(value = """
        SELECT pa.id_plan_action                   AS id,
               pa.code                             AS code,
               pa.designation                      AS designation,
               pa.date_fin_prevue                  AS dateFinPrevue,
               sta.code                            AS statutCode,
               sta.libelle                         AS statutLibelle,
               CAST(COALESCE(av.valeur_pourcentage, 0) AS DOUBLE PRECISION) AS avancement
        FROM plan_action pa
        LEFT JOIN LATERAL (
            SELECT av2.valeur_pourcentage, av2.id_statut
            FROM avancement_plan_action av2
            WHERE av2.id_plan_action = pa.id_plan_action
            ORDER BY av2.date_changement DESC, av2.id_avancement_plan_action DESC
            LIMIT 1
        ) av ON TRUE
        LEFT JOIN statut sta ON sta.id_statut = av.id_statut
        WHERE pa.id_plan_action = :id
        """, nativeQuery = true)
    Optional<PlanActionContexteRow> findPlanActionContexte(@Param("id") Integer idPlanAction);

    @Query(value = """
        SELECT i.id_indicateur                     AS id,
               i.code                              AS code,
               i.designation                       AS designation,
               v_courante.valeur                   AS valeurCourante,
               v_courante.periode_debut            AS periodeDebut,
               v_courante.periode_fin              AS periodeFin,
               i.seuil_min                         AS seuilMin,
               i.seuil_max                         AS seuilMax
        FROM indicateur i
        LEFT JOIN LATERAL (
            SELECT vi.valeur, vi.periode_debut, vi.periode_fin
            FROM valeur_indicateur vi
            WHERE vi.id_indicateur = i.id_indicateur
            ORDER BY vi.periode_fin DESC, vi.id_valeur_indicateur DESC
            LIMIT 1
        ) v_courante ON TRUE
        WHERE i.id_indicateur = :id
        """, nativeQuery = true)
    Optional<IndicateurContexteRow> findIndicateurContexte(@Param("id") Integer idIndicateur);
}