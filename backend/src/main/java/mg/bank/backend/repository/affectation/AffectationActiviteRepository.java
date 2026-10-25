package mg.bank.backend.repository.affectation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mg.bank.backend.model.affectation.AffectationActivite;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.projection.activite.ActiviteListRow;
import mg.bank.backend.repository.projection.affectation.AffectationRow;

@Repository
public interface AffectationActiviteRepository
        extends JpaRepository<AffectationActivite, Integer> {

    /** Historique complet des affectations d'une activite (actives et passees). */
    @Query(value = """
            SELECT aa.id_affectation_activite AS id,
                   aa.date_affectation        AS dateAffectation,
                   aa.date_desaffectation     AS dateDesaffectation,
                   r.id_role                  AS roleId,
                   r.code                     AS roleCode,
                   r.designation              AS roleLibelle,
                   u.id_utilisateur           AS utilisateurId,
                   u.nom                      AS utilisateurNom,
                   u.prenom                   AS utilisateurPrenom
            FROM affectation_activite aa
            JOIN role r        ON r.id_role = aa.id_role
            JOIN utilisateur u ON u.id_utilisateur = aa.id_utilisateur
            WHERE aa.id_activite = :idActivite
            ORDER BY aa.date_affectation DESC, aa.id_affectation_activite DESC
            """, nativeQuery = true)
    List<AffectationRow> findHistorique(@Param("idActivite") Integer idActivite);

    /** Une affectation active existe-t-elle sur cette activite ? */
    @Query(value = """
            SELECT COUNT(*) FROM affectation_activite
            WHERE id_activite = :idActivite
              AND date_desaffectation IS NULL
            """, nativeQuery = true)
    long countActives(@Param("idActivite") Integer idActivite);

    /** Une affectation active existe-t-elle pour ce couple (activite, role) ? */
    @Query(value = """
            SELECT COUNT(*) FROM affectation_activite
            WHERE id_activite = :idActivite
              AND id_role = :idRole
              AND date_desaffectation IS NULL
            """, nativeQuery = true)
    long countActivesPourRole(@Param("idActivite") Integer idActivite,
                              @Param("idRole") Integer idRole);

    /** Y a-t-il un RESPONSABLE actif sur cette activite ? */
    @Query(value = """
            SELECT COUNT(*) FROM affectation_activite aa
            JOIN role r ON r.id_role = aa.id_role
            WHERE aa.id_activite = :idActivite
              AND r.code = 'RESPONSABLE'
              AND aa.date_desaffectation IS NULL
            """, nativeQuery = true)
    long countResponsableActif(@Param("idActivite") Integer idActivite);

    /** L'affectation appartient-elle bien a l'activite ? (controle 404/403). */
    @Query(value = """
            SELECT COUNT(*) FROM affectation_activite
            WHERE id_affectation_activite = :idAffectation
              AND id_activite = :idActivite
            """, nativeQuery = true)
    long countAppartenance(@Param("idActivite") Integer idActivite,
                           @Param("idAffectation") Integer idAffectation);

    Optional<AffectationActivite> findByIdAffectationActiviteAndActiviteIdActivite(
            Integer idAffectation, Integer idActivite);

    /**
     * Liste des activites NON_COMMENCEE sans affectation active, dans le
     * perimetre de l'appelant. Les filtres sont ceux de la liste principale
     * (ActiviteRepository.FILTRES), plus deux contraintes :
     * - statut courant = NON_COMMENCEE ;
     * - pas d'affectation_activite active.
     */
    @Query(value = """
            SELECT act.id_activite                       AS id,
                   act.code                              AS code,
                   act.reference                         AS reference,
                   act.designation                       AS designation,
                   act.date_debut_prevue                 AS dateDebutPrevue,
                   act.date_fin_prevue                   AS dateFinPrevue,
                   act.date_debut_reelle                 AS dateDebutReelle,
                   act.date_fin_reelle                   AS dateFinReelle,
                   crea.date_creation                    AS dateCreation,
                   COALESCE(crea.date_creation, TIMESTAMP '-infinity') AS dateCreationTri,
                   os.id_objectif_specifique             AS osId,
                   os.code                               AS osCode,
                   os.designation                        AS osDesignation,
                   os.annee                              AS osAnnee,
                   srv.id_service                        AS serviceId,
                   srv.nom                               AS serviceLibelle,
                   ta.id_type_activite                   AS typeActiviteId,
                   ta.designation                        AS typeActiviteLibelle,
                   st.id_site                            AS siteId,
                   st.nom                                AS siteLibelle,
                   pr.id_priorite                        AS prioriteId,
                   pr.code                               AS prioriteCode,
                   pr.libelle                            AS prioriteLibelle,
                   sta.id_statut                         AS statutId,
                   sta.code                              AS statutCode,
                   sta.libelle                           AS statutLibelle,
                   COALESCE(av.avancement, 0)            AS avancement
            """
            + mg.bank.backend.repository.activite.ActiviteRepository.SELECT_BASE
            + mg.bank.backend.repository.activite.ActiviteRepository.FILTRES
            + """
              AND sta.code = 'NON_COMMENCEE'
              AND NOT EXISTS (
                  SELECT 1 FROM affectation_activite aa
                  WHERE aa.id_activite = act.id_activite
                    AND aa.date_desaffectation IS NULL
              )
            """, countQuery = """
            SELECT COUNT(*)
            """
            + mg.bank.backend.repository.activite.ActiviteRepository.SELECT_BASE
            + mg.bank.backend.repository.activite.ActiviteRepository.FILTRES
            + """
              AND sta.code = 'NON_COMMENCEE'
              AND NOT EXISTS (
                  SELECT 1 FROM affectation_activite aa
                  WHERE aa.id_activite = act.id_activite
                    AND aa.date_desaffectation IS NULL
              )
            """, nativeQuery = true)
    org.springframework.data.domain.Page<ActiviteListRow> findActivitesNonAffectees(
            @Param("pta") Boolean pta,
            @Param("recherche") String recherche,
            @Param("objectifId") Integer objectifId,
            @Param("objectifRecherche") String objectifRecherche,
            @Param("servicePerimetre") Integer servicePerimetre,
            @Param("departementPerimetre") Integer departementPerimetre,
            @Param("serviceDemande") Integer serviceDemande,
            @Param("prioriteId") Integer prioriteId,
            @Param("typeActiviteId") Integer typeActiviteId,
            @Param("siteId") Integer siteId,
            @Param("statutCode") String statutCode,
            @Param("codesTerminaux") String codesTerminaux,
            @Param("annee") Integer annee,
            @Param("moisDebut") Integer moisDebut,
            @Param("moisFin") Integer moisFin,
            @Param("dateDebut") java.time.LocalDate dateDebut,
            @Param("dateFin") java.time.LocalDate dateFin,
            org.springframework.data.domain.Pageable pageable);
}