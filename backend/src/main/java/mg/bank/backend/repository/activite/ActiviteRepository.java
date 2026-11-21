package mg.bank.backend.repository.activite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.repository.SqlFragments;
import mg.bank.backend.repository.projection.activite.ActiviteDetailRow;
import mg.bank.backend.repository.projection.activite.ActiviteListRow;
import mg.bank.backend.repository.projection.activite.ActiviteStatistiquesRow;
import mg.bank.backend.repository.projection.activite.ActiviteSuiviStatistiquesRow;
import mg.bank.backend.repository.projection.activite.AvancementDetailRow;
import mg.bank.backend.repository.projection.activite.FichierDetailRow;
import mg.bank.backend.repository.projection.activite.FichierTelechargementRow;
import mg.bank.backend.repository.projection.activite.ActiviteExportRow;
import mg.bank.backend.repository.projection.activite.HistoriqueActiviteRow;
import mg.bank.backend.repository.projection.activite.LivrableDetailRow;
import mg.bank.backend.repository.projection.activite.ResponsableExportRow;
import mg.bank.backend.repository.projection.activite.ResponsableSuiviRow;
import mg.bank.backend.repository.projection.activite.ResultatIntermediaireRow;
import mg.bank.backend.repository.projection.activite.SousActiviteDetailRow;
import mg.bank.backend.repository.projection.activite.SousActiviteExportRow;
import mg.bank.backend.repository.projection.activite.SousActiviteSuiviRow;
import mg.bank.backend.repository.projection.activite.SuiviExportRow;
import mg.bank.backend.repository.projection.activite.UtilisateurActiviteAnneeRow;
import mg.bank.backend.repository.projection.affectation.AffectationDetailRow;
import mg.bank.backend.repository.projection.common.OptionRow;
import mg.bank.backend.repository.projection.common.StatutCourantRow;
import mg.bank.backend.repository.projection.indicateur.IndicateurDetailRow;
import mg.bank.backend.repository.projection.indicateur.ValeurIndicateurRow;

public interface ActiviteRepository extends JpaRepository<Activite, Integer> {

       String SQL_EN_COURS = "'" + StatutsActivite.EN_COURS + "'";
       String SQL_TERMINEE = "'" + StatutsActivite.TERMINEE + "'";
       String SQL_ANNULEE = "'" + StatutsActivite.ANNULEE + "'";
       String SQL_REPORTEE = "'" + StatutsActivite.REPORTEE + "'";
       String SQL_EN_RETARD = "'" + StatutsActivite.EN_RETARD + "'";
       String SQL_NON_COMMENCEE = "'" + StatutsActivite.NON_COMMENCEE + "'";
       String SQL_SUSPENDUE = "'" + StatutsActivite.SUSPENDUE + "'";

       // La liste publique masque les brouillons et les états du circuit de validation.
       // Le dashboard les distingue : il exclut les brouillons mais conserve l'attente de validation.
       String SQL_NON_PUBLIEES = "'" + StatutsActivite.BROUILLON + "', '"
                     + StatutsActivite.EN_ATTENTE_VALIDATION + "', '"
                     + StatutsActivite.VALIDEE + "', '"
                     + StatutsActivite.REJETE + "'";

       String SELECT_BASE = """
                     FROM activite act
                     LEFT JOIN service srv
                            ON srv.id_service = act.id_service
                     LEFT JOIN objectif_specifique os
                            ON os.id_objectif_specifique = act.id_objectif_specifique
                     LEFT JOIN type_activite ta
                            ON ta.id_type_activite = act.id_type_activite
                     LEFT JOIN site st
                            ON st.id_site = act.id_site
                     LEFT JOIN priorite pr
                            ON pr.id_priorite = act.id_priorite
                     """
                     + SqlFragments.STATUT_COURANT
                     + """
                                   LEFT JOIN LATERAL (
                                       SELECT MIN(h2.date_changement) AS date_creation
                                       FROM historique_activite h2
                                       WHERE h2.id_activite = act.id_activite
                                   ) crea ON TRUE
                                   """
                     + SqlFragments.AVANCEMENT_ACTIVITE_COURANT;

       String SELECT_BASE_COUNT = """
                     FROM activite act
                     LEFT JOIN service srv
                            ON srv.id_service = act.id_service
                     LEFT JOIN objectif_specifique os
                            ON os.id_objectif_specifique = act.id_objectif_specifique
                     LEFT JOIN type_activite ta
                            ON ta.id_type_activite = act.id_type_activite
                     LEFT JOIN site st
                            ON st.id_site = act.id_site
                     LEFT JOIN priorite pr
                            ON pr.id_priorite = act.id_priorite
                     """
                     + SqlFragments.STATUT_COURANT;

       String FILTRES = """
                     WHERE 1 = 1

                     AND (:pta IS NULL
                            OR (CAST(:pta AS BOOLEAN) AND act.id_objectif_specifique IS NOT NULL)
                            OR (NOT CAST(:pta AS BOOLEAN) AND act.id_objectif_specifique IS NULL))

                     AND (:recherche IS NULL
                            OR act.code ILIKE :recherche
                            OR act.reference ILIKE :recherche
                            OR act.designation ILIKE :recherche)

                     AND (:objectifId IS NULL
                            OR os.id_objectif_specifique = :objectifId)
                     AND (:objectifRecherche IS NULL
                            OR os.code ILIKE :objectifRecherche
                            OR os.designation ILIKE :objectifRecherche)

                     AND (:servicePerimetre IS NULL
                            OR act.id_service = :servicePerimetre)
                     AND (:departementPerimetre IS NULL
                            OR srv.id_departement = :departementPerimetre)
                     AND (:serviceDemande IS NULL
                            OR act.id_service = :serviceDemande)

                     AND (:prioriteId IS NULL
                            OR pr.id_priorite = :prioriteId)
                     AND (:typeActiviteId IS NULL
                            OR ta.id_type_activite = :typeActiviteId)
                     AND (:siteId IS NULL
                            OR st.id_site = :siteId)

                     AND (((:statutCode IS NOT NULL AND :statutCode = sta.code)

                     OR ((:statutCode IS NULL
                            OR (CASE
                                   WHEN :statutCode = """
                     + SQL_EN_RETARD
                     + """
                                                        THEN act.date_fin_prevue IS NOT NULL
                                                        AND act.date_fin_prevue < CURRENT_DATE
                                                        AND (sta.code IS NULL
                                                        OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ','))))
                                                 ELSE sta.code = :statutCode
                                                 END))

                                   AND (sta.code IS NULL
                                          OR sta.code NOT IN ("""
                     + SQL_NON_PUBLIEES
                     + """
                                   )))))

                            
                                   AND (sta.code IS NULL
                                          OR sta.code NOT IN ("""
                     + SQL_NON_PUBLIEES
                     + """
                                   ))

                                   AND (:annee IS NULL
                                          OR (act.date_debut_prevue <= MAKE_DATE(:annee, 12, 31)
                                              AND COALESCE(act.date_fin_prevue, act.date_debut_prevue)
                                                  >= MAKE_DATE(:annee, 1, 1)))
                                   AND (:moisDebut IS NULL
                                          OR (act.date_debut_prevue <= (
                                                  MAKE_DATE(COALESCE(:annee, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER), :moisFin, 1)
                                                  + INTERVAL '1 month - 1 day')::DATE
                                              AND COALESCE(act.date_fin_prevue, act.date_debut_prevue)
                                                  >= MAKE_DATE(COALESCE(:annee, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER), :moisDebut, 1)))

                                   AND (CAST(:dateDebut AS DATE) IS NULL
                                          OR act.date_fin_prevue IS NULL
                                          OR act.date_fin_prevue >= CAST(:dateDebut AS DATE))
                                   AND (CAST(:dateFin AS DATE) IS NULL
                                          OR act.date_debut_prevue <= CAST(:dateFin AS DATE))
                                   """;

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
                     + SELECT_BASE
                     + FILTRES, countQuery = "SELECT COUNT(*) " + SELECT_BASE_COUNT + FILTRES, nativeQuery = true)
       Page<ActiviteListRow> findActivites(
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
                     Pageable pageable);

       @Query(value = """
                     SELECT COUNT(*)                                                  AS total,
                            COUNT(*) FILTER (WHERE sta.code = """
                     + SQL_NON_COMMENCEE
                     + """
                                   )    AS nonCommencees,
                                          COUNT(*) FILTER (WHERE sta.code = """
                     + SQL_EN_COURS
                     + """
                                   )    AS enCours,
                                          COUNT(*) FILTER (WHERE sta.code = """
                     + SQL_TERMINEE
                     + """
                                   )    AS terminees,
                                          COUNT(*) FILTER (WHERE sta.code = """
                     + SQL_ANNULEE
                     + """
                                   )    AS annulees,
                                          COUNT(*) FILTER (WHERE sta.code = """
                     + SQL_REPORTEE
                     + """
                                   )    AS reportees,
                                          COUNT(*) FILTER (WHERE sta.code = """
                     + SQL_SUSPENDUE
                     + """
                                   )    AS suspendues,
                                          COUNT(*) FILTER (
                                              WHERE act.date_fin_prevue IS NOT NULL
                                                AND act.date_fin_prevue < CURRENT_DATE
                                                AND (sta.code IS NULL
                                                     OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ',')))))
                                                                                                   AS enRetard
                                   """
                     + SELECT_BASE
                     + FILTRES, nativeQuery = true)
       ActiviteStatistiquesRow statistiques(
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
                     @Param("dateFin") java.time.LocalDate dateFin);

          /** Agrégats du suivi calculés sur tout le résultat filtré, sans pagination. */
          @Query(value = """
                                    SELECT COUNT(*) AS totalActivites,
                                                 COALESCE(AVG(COALESCE(av.avancement, 0)), 0) AS avancementMoyen,
                                                 COALESCE(SUM(sous.total), 0) AS totalSousActivites,
                                                 COALESCE(SUM(sous.terminees), 0) AS sousActivitesTerminees
                                    """
                                    + SELECT_BASE
                                    + """
                                    LEFT JOIN LATERAL (
                                           SELECT COUNT(*) AS total,
                                                        COUNT(*) FILTER (
                                                               WHERE COALESCE(dernier.valeur_pourcentage, 0) = 100
                                                        ) AS terminees
                                           FROM sous_activite sa
                                           LEFT JOIN LATERAL (
                                                  SELECT av2.valeur_pourcentage
                                                  FROM avancement_sous_activite av2
                                                  WHERE av2.id_sous_activite = sa.id_sous_activite
                                                  ORDER BY av2.date_changement DESC,
                                                                 av2.id_historique_sous_activite DESC
                                                  LIMIT 1
                                           ) dernier ON TRUE
                                           WHERE sa.id_activite = act.id_activite
                                    ) sous ON TRUE
                                    """
                                    + FILTRES, nativeQuery = true)
          ActiviteSuiviStatistiquesRow statistiquesSuivi(
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
                                    @Param("dateFin") java.time.LocalDate dateFin);

       @Query(value = """
                     SELECT act.id_activite AS id,
                            act.code        AS code,
                            act.designation AS libelle,
                            ta.designation  AS libelleSecondaire
                     FROM activite act
                     LEFT JOIN type_activite ta
                            ON ta.id_type_activite = act.id_type_activite
                     WHERE (:recherche IS NULL
                            OR act.code ILIKE :motif
                            OR act.reference ILIKE :motif
                            OR act.designation ILIKE :motif)
                                                   AND (:pta IS NULL
                                                                OR (CAST(:pta AS BOOLEAN) AND act.id_objectif_specifique IS NOT NULL)
                                                                OR (NOT CAST(:pta AS BOOLEAN) AND act.id_objectif_specifique IS NULL))
                       AND (:servicePerimetre IS NULL
                            OR act.id_service = :servicePerimetre)
                       AND (:departementPerimetre IS NULL
                            OR EXISTS (SELECT 1 FROM service s
                                       WHERE s.id_service = act.id_service
                                         AND s.id_departement = :departementPerimetre))
                       AND NOT EXISTS (
                           SELECT 1
                           FROM historique_activite h
                           JOIN statut s
                                  ON s.id_statut = h.id_statut
                           WHERE h.id_activite = act.id_activite
                             AND NOT EXISTS (
                                 SELECT 1
                                 FROM historique_activite h2
                                 WHERE h2.id_activite = act.id_activite
                                   AND (h2.date_changement, h2.id_historique_activite)
                                       > (h.date_changement, h.id_historique_activite))
                             AND s.code IN ("""
                     + SQL_NON_PUBLIEES
                     + """
                                          )
                                     )
                                   ORDER BY act.code
                                   LIMIT :limite
                                   """, nativeQuery = true)
       List<OptionRow> autocomplete(
                     @Param("recherche") String recherche,
                     @Param("motif") String motif,
                     @Param("pta") Boolean pta,
                     @Param("servicePerimetre") Integer servicePerimetre,
                     @Param("departementPerimetre") Integer departementPerimetre,
                     @Param("limite") int limite);

       // ------------------------------------------------------------------
       // Detail
       // ------------------------------------------------------------------
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
                            COALESCE(av.avancement, 0)            AS avancement,
                            (act.date_fin_prevue IS NOT NULL
                             AND act.date_fin_prevue < CURRENT_DATE
                             AND (sta.code IS NULL
                                  OR sta.code NOT IN (SELECT unnest(string_to_array(:codesTerminaux, ',')))))
                                                                                          AS enRetard
                     """
                     + SELECT_BASE
                     + """
                                   WHERE act.id_activite = :id
                                     AND (:servicePerimetre IS NULL
                                          OR act.id_service = :servicePerimetre)
                                     AND (:departementPerimetre IS NULL
                                          OR srv.id_departement = :departementPerimetre)
                                   """, nativeQuery = true)
       Optional<ActiviteDetailRow> findActiviteDetail(
                     @Param("id") Integer id,
                     @Param("servicePerimetre") Integer servicePerimetre,
                     @Param("departementPerimetre") Integer departementPerimetre,
                     @Param("codesTerminaux") String codesTerminaux);

       @Query(value = """
                     SELECT COUNT(*)
                     FROM activite act
                     LEFT JOIN service srv
                            ON srv.id_service = act.id_service
                     WHERE act.id_activite = :id
                     """, nativeQuery = true)
       long compterActivite(@Param("id") Integer id);

       @Query(value = """
                     SELECT act.id_activite AS id,
                            sta.code       AS statutCode
                     """
                     + SELECT_BASE
                     + """
                                   WHERE act.id_activite = :id
                                     AND (:servicePerimetre IS NULL
                                          OR act.id_service = :servicePerimetre)
                                     AND (:departementPerimetre IS NULL
                                          OR srv.id_departement = :departementPerimetre)
                                   """, nativeQuery = true)
       Optional<StatutCourantRow> findStatutCourant(
                     @Param("id") Integer id,
                     @Param("servicePerimetre") Integer servicePerimetre,
                     @Param("departementPerimetre") Integer departementPerimetre);

       boolean existsByCodeIgnoreCaseAndService_IdService(String code, Integer idService);

       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT a FROM Activite a WHERE a.idActivite = :id")
       Optional<Activite> verrouillerActivite(@Param("id") Integer id);

       @Modifying
       @Query(value = """
                     INSERT INTO historique_activite
                         (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
                     VALUES (
                         :commentaire,
                         GREATEST(
                             CURRENT_TIMESTAMP,
                             COALESCE(
                                 (SELECT max(h.date_changement)
                                  FROM historique_activite h
                                  WHERE h.id_activite = :idActivite),
                                 CURRENT_TIMESTAMP) + make_interval(secs => 1)),
                         :idUtilisateur,
                         (SELECT id_statut FROM statut WHERE code = :codeStatut),
                         :idActivite)
                     """, nativeQuery = true)
       int ajouterChangementStatut(
                     @Param("idActivite") Integer idActivite,
                     @Param("idUtilisateur") Integer idUtilisateur,
                     @Param("codeStatut") String codeStatut,
                     @Param("commentaire") String commentaire);

       @Query(value = """
                     SELECT sa.id_sous_activite  AS id,
                            sa.code              AS code,
                            sa.designation       AS designation,
                            sa.date_debut_prevue AS dateDebutPrevue,
                            sa.date_fin_prevue   AS dateFinPrevue,
                            sa.date_debut_reelle AS dateDebutReelle,
                            sa.date_fin_reelle   AS dateFinReelle
                     FROM sous_activite sa
                     WHERE sa.id_activite = :id
                     ORDER BY sa.date_debut_prevue, sa.code
                     """, nativeQuery = true)
       List<SousActiviteDetailRow> findSousActivites(@Param("id") Integer id);

       @Query(value = """
                     SELECT av.id_historique_sous_activite AS id,
                            av.id_sous_activite             AS idSousActivite,
                            av.valeur_pourcentage           AS valeurPourcentage,
                            av.commentaire                  AS commentaire,
                            av.date_changement              AS dateChangement,
                            sta.id_statut                   AS statutId,
                            sta.code                        AS statutCode,
                            sta.libelle                     AS statutLibelle,
                            u.id_utilisateur                AS utilisateurId,
                            u.nom                           AS utilisateurNom,
                            u.prenom                        AS utilisateurPrenom
                     FROM avancement_sous_activite av
                     JOIN sous_activite sa
                            ON sa.id_sous_activite = av.id_sous_activite
                     LEFT JOIN statut sta
                            ON sta.id_statut = av.id_statut
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = av.id_utilisateur
                     WHERE sa.id_activite = :id
                     ORDER BY sa.id_sous_activite,
                              av.date_changement DESC,
                              av.id_historique_sous_activite DESC
                     """, nativeQuery = true)
       List<AvancementDetailRow> findAvancements(@Param("id") Integer id);

       @Query(value = """
                     SELECT a.id_affectation_sous_activite AS id,
                            a.id_sous_activite            AS idSousActivite,
                            a.date_affectation            AS dateAffectation,
                            a.date_desaffectation         AS dateDesaffectation,
                            r.id_role                     AS roleId,
                            r.code                        AS roleCode,
                            r.designation                 AS roleLibelle,
                            u.id_utilisateur              AS utilisateurId,
                            u.nom                         AS utilisateurNom,
                            u.prenom                      AS utilisateurPrenom
                     FROM affectation_sous_activite a
                     JOIN sous_activite sa
                            ON sa.id_sous_activite = a.id_sous_activite
                     LEFT JOIN role r
                            ON r.id_role = a.id_role
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = a.id_utilisateur
                     WHERE sa.id_activite = :id
                     ORDER BY sa.id_sous_activite, a.date_affectation DESC
                     """, nativeQuery = true)
       List<AffectationDetailRow> findAffectations(@Param("id") Integer id);

       @Query(value = """
                     SELECT l.id_livrable_sous_activite AS id,
                            l.id_sous_activite           AS idSousActivite,
                            l.designation                AS designation,
                            l.description                AS description
                     FROM livrable_sous_activite l
                     JOIN sous_activite sa
                            ON sa.id_sous_activite = l.id_sous_activite
                     WHERE sa.id_activite = :id
                     ORDER BY sa.id_sous_activite, l.designation, l.id_livrable_sous_activite
                     """, nativeQuery = true)
       List<LivrableDetailRow> findLivrables(@Param("id") Integer id);

       @Query(value = """
                     SELECT f.id_fichier_sous_activite    AS id,
                            f.id_livrable_sous_activite   AS idLivrable,
                            f.nom_fichier                 AS nomFichier,
                            f.nom_original                AS nomOriginal,
                            f.extension                   AS extension,
                            f.type_mime                   AS typeMime,
                            f.taille                      AS taille,
                            f.version                     AS version,
                            f.date_depot                  AS dateDepot,
                            u.id_utilisateur              AS utilisateurId,
                            u.nom                         AS utilisateurNom,
                            u.prenom                      AS utilisateurPrenom
                     FROM fichier_sous_activite f
                     JOIN livrable_sous_activite l
                            ON l.id_livrable_sous_activite = f.id_livrable_sous_activite
                     JOIN sous_activite sa
                            ON sa.id_sous_activite = l.id_sous_activite
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = f.id_utilisateur
                     WHERE sa.id_activite = :id
                     ORDER BY l.id_livrable_sous_activite,
                              f.nom_original,
                              f.version DESC
                     """, nativeQuery = true)
       List<FichierDetailRow> findFichiers(@Param("id") Integer id);

       @Query(value = """
                     SELECT i.id_indicateur              AS id,
                            i.code                       AS code,
                            i.designation                AS designation,
                            i.type_indicateur            AS typeIndicateur,
                            i.unite_mesure               AS uniteMesure,
                            i.frequence_verification     AS frequenceVerification,
                            i.frequence_aggregation      AS frequenceAggregation,
                            i.definition                 AS definition,
                            i.methode_determination      AS methodeDetermination,
                            i.objectif                   AS objectif,
                            i.valeur_cible               AS valeurCible,
                            i.seuil_min                  AS seuilMin,
                            i.seuil_max                  AS seuilMax,
                            i.actif                      AS actif
                     FROM activite_indicateur ai
                     JOIN indicateur i
                            ON i.id_indicateur = ai.id_indicateur
                     WHERE ai.id_activite = :id
                     ORDER BY i.code
                     """, nativeQuery = true)
       List<IndicateurDetailRow> findIndicateurs(@Param("id") Integer id);

       @Query(value = """
                     SELECT v.id_valeur_indicateur AS id,
                            v.id_indicateur        AS idIndicateur,
                            v.valeur               AS valeur,
                            v.periode_debut        AS periodeDebut,
                            v.periode_fin          AS periodeFin,
                            v.commentaire          AS commentaire,
                            v.date_saisie          AS dateSaisie,
                            u.id_utilisateur       AS utilisateurId,
                            u.nom                  AS utilisateurNom,
                            u.prenom               AS utilisateurPrenom
                     FROM valeur_indicateur v
                     JOIN activite_indicateur ai
                            ON ai.id_indicateur = v.id_indicateur
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = v.id_utilisateur
                     WHERE ai.id_activite = :id
                     ORDER BY v.id_indicateur,
                              v.periode_fin DESC,
                              v.id_valeur_indicateur DESC
                     """, nativeQuery = true)
       List<ValeurIndicateurRow> findValeursIndicateurs(@Param("id") Integer id);

       /**
        * Resultats intermediaires de l activite, tries par designation.
        */
       @Query(value = """
                     SELECT r.id_resultat_intermediaire AS id,
                            r.designation              AS designation
                     FROM resultat_intermediaire r
                     WHERE r.id_activite = :id
                     ORDER BY r.designation, r.id_resultat_intermediaire
                     """, nativeQuery = true)
       List<ResultatIntermediaireRow> findResultatsIntermediaires(@Param("id") Integer id);

       @Query(value = """
                     SELECT h.id_historique_activite AS id,
                            h.date_changement        AS dateChangement,
                            h.commentaire            AS commentaire,
                            sta.id_statut             AS statutId,
                            sta.code                  AS statutCode,
                            sta.libelle               AS statutLibelle,
                            u.id_utilisateur          AS utilisateurId,
                            u.nom                     AS utilisateurNom,
                            u.prenom                  AS utilisateurPrenom
                     FROM historique_activite h
                     LEFT JOIN statut sta
                            ON sta.id_statut = h.id_statut
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = h.id_utilisateur
                     WHERE h.id_activite = :id
                     ORDER BY h.date_changement DESC, h.id_historique_activite DESC
                     """, nativeQuery = true)
       List<HistoriqueActiviteRow> findHistorique(@Param("id") Integer id);

       // ------------------------------------------------------------------
       // Detail d une sous-activite
       // ------------------------------------------------------------------
       @Query(value = """
                     SELECT sa.id_sous_activite  AS id,
                            sa.code              AS code,
                            sa.designation       AS designation,
                            sa.date_debut_prevue AS dateDebutPrevue,
                            sa.date_fin_prevue   AS dateFinPrevue,
                            sa.date_debut_reelle AS dateDebutReelle,
                            sa.date_fin_reelle   AS dateFinReelle,
                            sta.code             AS statutActivite
                     FROM sous_activite sa
                     JOIN activite act
                            ON act.id_activite = sa.id_activite
                     LEFT JOIN service srv
                            ON srv.id_service = act.id_service
                     LEFT JOIN LATERAL (
                         SELECT h.id_statut
                         FROM historique_activite h
                         WHERE h.id_activite = act.id_activite
                           AND NOT EXISTS (
                               SELECT 1
                               FROM historique_activite h2
                               WHERE h2.id_activite = act.id_activite
                                 AND (h2.date_changement, h2.id_historique_activite)
                                     > (h.date_changement, h.id_historique_activite))
                     ) hist ON TRUE
                     LEFT JOIN statut sta
                            ON sta.id_statut = hist.id_statut
                     WHERE sa.id_sous_activite = :idSousActivite
                       AND act.id_activite = :idActivite
                       AND (:servicePerimetre IS NULL
                            OR act.id_service = :servicePerimetre)
                       AND (:departementPerimetre IS NULL
                            OR srv.id_departement = :departementPerimetre)
                     """, nativeQuery = true)
       Optional<SousActiviteDetailRow> findSousActivite(
                     @Param("idActivite") Integer idActivite,
                     @Param("idSousActivite") Integer idSousActivite,
                     @Param("servicePerimetre") Integer servicePerimetre,
                     @Param("departementPerimetre") Integer departementPerimetre);

       @Query(value = """
                     SELECT COUNT(*)
                     FROM sous_activite
                     WHERE id_sous_activite = :id
                     """, nativeQuery = true)
       long compterSousActivite(@Param("id") Integer id);

       @Query(value = """
                     SELECT COUNT(*)
                     FROM sous_activite
                     WHERE id_sous_activite = :idSousActivite
                       AND id_activite = :idActivite
                     """, nativeQuery = true)
       long compterSousActiviteDeActivite(
                     @Param("idActivite") Integer idActivite,
                     @Param("idSousActivite") Integer idSousActivite);

       @Query(value = """
                     SELECT av.id_historique_sous_activite AS id,
                            av.id_sous_activite             AS idSousActivite,
                            av.valeur_pourcentage           AS valeurPourcentage,
                            av.commentaire                  AS commentaire,
                            av.date_changement              AS dateChangement,
                            sta.id_statut                   AS statutId,
                            sta.code                        AS statutCode,
                            sta.libelle                     AS statutLibelle,
                            u.id_utilisateur                AS utilisateurId,
                            u.nom                           AS utilisateurNom,
                            u.prenom                        AS utilisateurPrenom
                     FROM avancement_sous_activite av
                     LEFT JOIN statut sta
                            ON sta.id_statut = av.id_statut
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = av.id_utilisateur
                     WHERE av.id_sous_activite = :id
                     ORDER BY av.date_changement DESC,
                              av.id_historique_sous_activite DESC
                     """, nativeQuery = true)
       List<AvancementDetailRow> findAvancementsSousActivite(@Param("id") Integer id);

       /**
        * Affectations d UNE sous-activite, les plus recentes d abord.
        */
       @Query(value = """
                     SELECT a.id_affectation_sous_activite AS id,
                            a.id_sous_activite            AS idSousActivite,
                            a.date_affectation            AS dateAffectation,
                            a.date_desaffectation         AS dateDesaffectation,
                            r.id_role                     AS roleId,
                            r.code                        AS roleCode,
                            r.designation                 AS roleLibelle,
                            u.id_utilisateur              AS utilisateurId,
                            u.nom                         AS utilisateurNom,
                            u.prenom                      AS utilisateurPrenom
                     FROM affectation_sous_activite a
                     LEFT JOIN role r
                            ON r.id_role = a.id_role
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = a.id_utilisateur
                     WHERE a.id_sous_activite = :id
                     ORDER BY a.date_affectation DESC
                     """, nativeQuery = true)
       List<AffectationDetailRow> findAffectationsSousActivite(@Param("id") Integer id);

       /**
        * Livrables d UNE sous-activite, tries par designation.
        */
       @Query(value = """
                     SELECT l.id_livrable_sous_activite AS id,
                            l.id_sous_activite           AS idSousActivite,
                            l.designation                AS designation,
                            l.description                AS description
                     FROM livrable_sous_activite l
                     WHERE l.id_sous_activite = :id
                     ORDER BY l.designation, l.id_livrable_sous_activite
                     """, nativeQuery = true)
       List<LivrableDetailRow> findLivrablesSousActivite(@Param("id") Integer id);

       @Query(value = """
                     SELECT f.id_fichier_sous_activite    AS id,
                            f.id_livrable_sous_activite   AS idLivrable,
                            f.nom_fichier                 AS nomFichier,
                            f.nom_original                AS nomOriginal,
                            f.extension                   AS extension,
                            f.type_mime                   AS typeMime,
                            f.taille                      AS taille,
                            f.version                     AS version,
                            f.date_depot                  AS dateDepot,
                            u.id_utilisateur              AS utilisateurId,
                            u.nom                         AS utilisateurNom,
                            u.prenom                      AS utilisateurPrenom
                     FROM fichier_sous_activite f
                     JOIN livrable_sous_activite l
                            ON l.id_livrable_sous_activite = f.id_livrable_sous_activite
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = f.id_utilisateur
                     WHERE l.id_sous_activite = :id
                     ORDER BY l.id_livrable_sous_activite,
                              f.nom_original,
                              f.version DESC
                     """, nativeQuery = true)
       List<FichierDetailRow> findFichiersSousActivite(@Param("id") Integer id);

       @Query(value = """
                     SELECT f.id_fichier_sous_activite    AS id,
                            f.id_livrable_sous_activite   AS idLivrable,
                            f.nom_fichier                 AS nomFichier,
                            f.nom_original                AS nomOriginal,
                            f.extension                   AS extension,
                            f.type_mime                   AS typeMime,
                            f.taille                      AS taille,
                            f.version                     AS version,
                            f.date_depot                  AS dateDepot,
                            u.id_utilisateur              AS utilisateurId,
                            u.nom                         AS utilisateurNom,
                            u.prenom                      AS utilisateurPrenom
                     FROM fichier_sous_activite f
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = f.id_utilisateur
                     WHERE f.id_livrable_sous_activite = :id
                     ORDER BY f.nom_original,
                              f.version DESC
                     """, nativeQuery = true)
       List<FichierDetailRow> findFichiersLivrable(@Param("id") Integer id);

       // ------------------------------------------------------------------
       // Telechargement d un fichier
       // ------------------------------------------------------------------
       @Query(value = """
                     SELECT f.id_fichier_sous_activite AS id,
                            f.nom_fichier              AS nomFichier,
                            f.nom_original             AS nomOriginal,
                            f.chemin_fichier           AS cheminFichier,
                            f.extension                AS extension,
                            f.type_mime                AS typeMime,
                            f.taille                   AS taille,
                            f.version                  AS version,
                            f.date_depot               AS dateDepot
                     FROM fichier_sous_activite f
                     JOIN livrable_sous_activite l
                            ON l.id_livrable_sous_activite = f.id_livrable_sous_activite
                     JOIN sous_activite sa
                            ON sa.id_sous_activite = l.id_sous_activite
                     JOIN activite act
                            ON act.id_activite = sa.id_activite
                     LEFT JOIN service srv
                            ON srv.id_service = act.id_service
                     WHERE f.id_fichier_sous_activite = :idFichier
                       AND sa.id_sous_activite = :idSousActivite
                       AND act.id_activite = :idActivite
                       AND (:servicePerimetre IS NULL
                            OR act.id_service = :servicePerimetre)
                       AND (:departementPerimetre IS NULL
                            OR srv.id_departement = :departementPerimetre)
                     """, nativeQuery = true)
       Optional<FichierTelechargementRow> findFichierTelechargement(
                     @Param("idActivite") Integer idActivite,
                     @Param("idSousActivite") Integer idSousActivite,
                     @Param("idFichier") Integer idFichier,
                     @Param("servicePerimetre") Integer servicePerimetre,
                     @Param("departementPerimetre") Integer departementPerimetre);

       /**
        * Le fichier existe-t-il, quel que soit son rattachement ?
        *
        * Executee seulement sur le chemin d erreur, comme compterSousActivite.
        */
       @Query(value = """
                     SELECT COUNT(*)
                     FROM fichier_sous_activite
                     WHERE id_fichier_sous_activite = :id
                     """, nativeQuery = true)
       long compterFichier(@Param("id") Integer id);

       /**
        * Le fichier est-il rattache a cette sous-activite, par son livrable ?
        */
       @Query(value = """
                     SELECT COUNT(*)
                     FROM fichier_sous_activite f
                     JOIN livrable_sous_activite l
                            ON l.id_livrable_sous_activite = f.id_livrable_sous_activite
                     WHERE l.id_sous_activite = :idSousActivite
                       AND f.id_fichier_sous_activite = :idFichier
                     """, nativeQuery = true)
       long compterFichierDeSousActivite(
                     @Param("idSousActivite") Integer idSousActivite,
                     @Param("idFichier") Integer idFichier);

       @Query(value = """
                     SELECT a.*
                     FROM validation_activite v
                     JOIN activite a ON a.id_activite = v.id_activite
                     WHERE v.id_etape_validation = :idEtapeValidation
                       AND (CAST(:decision AS VARCHAR) IS NULL
                            OR v.decision = CAST(:decision AS decision_validation))
                     ORDER BY v.date_demande ASC
                     """, nativeQuery = true)
       List<Activite> findActivitesParEtape(
                     @Param("idEtapeValidation") Integer idEtapeValidation,
                     @Param("decision") String decision);

       @Query("""
                     SELECT a FROM Activite a
                     LEFT JOIN FETCH a.objectifSpecifique
                     LEFT JOIN FETCH a.service
                     LEFT JOIN FETCH a.typeActivite
                     LEFT JOIN FETCH a.site
                     LEFT JOIN FETCH a.priorite
                     WHERE EXISTS (
                         SELECT 1 FROM HistoriqueActivite h
                         WHERE h.activite = a
                           AND h.statut.code = :codeBrouillon
                           AND h.utilisateur.idUtilisateur = :idUtilisateur
                           AND NOT EXISTS (
                               SELECT 1 FROM HistoriqueActivite h2
                               WHERE h2.activite = a
                                 AND (h2.dateChangement > h.dateChangement
                                      OR (h2.dateChangement = h.dateChangement
                                          AND h2.idHistoriqueActivite > h.idHistoriqueActivite))
                           )
                     )
                     ORDER BY a.dateDebutPrevue ASC, a.idActivite ASC
                     """)
       List<Activite> findBrouillonsByUtilisateur(
                     @Param("codeBrouillon") String codeBrouillon,
                     @Param("idUtilisateur") Integer idUtilisateur);

       @Query(value = """
                     SELECT sa.id_sous_activite AS id,
                            sa.id_activite      AS idActivite,
                            sa.code             AS code,
                            sa.designation      AS designation
                     FROM sous_activite sa
                     WHERE sa.id_activite IN (:ids)
                     ORDER BY sa.id_activite, sa.date_debut_prevue, sa.code
                     """, nativeQuery = true)
       List<SousActiviteSuiviRow> findSousActivitesPourActivites(@Param("ids") List<Integer> ids);

       @Query(value = """
                     SELECT DISTINCT ON (act.id_activite)
                            act.id_activite     AS idActivite,
                            u.id_utilisateur    AS idUtilisateur,
                            u.nom               AS nom,
                            u.prenom            AS prenom
                     FROM activite act
                     JOIN sous_activite sa
                            ON sa.id_activite = act.id_activite
                     JOIN affectation_sous_activite aff
                            ON aff.id_sous_activite = sa.id_sous_activite
                           AND aff.date_desaffectation IS NULL
                     JOIN utilisateur u
                            ON u.id_utilisateur = aff.id_utilisateur
                     WHERE act.id_activite IN (:ids)
                     ORDER BY act.id_activite, aff.date_affectation ASC, aff.id_affectation_sous_activite ASC
                     """, nativeQuery = true)
       List<ResponsableSuiviRow> findResponsablesActifsPourActivites(@Param("ids") List<Integer> ids);

       @Query(value = """
                     SELECT COALESCE(AVG(COALESCE(dernier.valeur_pourcentage, 0)), 0) AS avancement
                     FROM sous_activite sa
                     LEFT JOIN LATERAL (
                         SELECT av.valeur_pourcentage
                         FROM avancement_sous_activite av
                         WHERE av.id_sous_activite = sa.id_sous_activite
                           AND NOT EXISTS (
                               SELECT 1 FROM avancement_sous_activite av2
                               WHERE av2.id_sous_activite = av.id_sous_activite
                                 AND (av2.date_changement, av2.id_historique_sous_activite)
                                     > (av.date_changement, av.id_historique_sous_activite))
                     ) dernier ON TRUE
                     WHERE sa.id_activite = :id
                     """, nativeQuery = true)
       Double findAvancementActivite(@Param("id") Integer idActivite);

       @Query(value = "SELECT pg_advisory_xact_lock(:cle)", nativeQuery = true)
       void verrouillerImport(@Param("cle") long cle);

       @Modifying
       @Query(value = "INSERT INTO activite_indicateur (id_activite, id_indicateur) "
                     + "VALUES (:idActivite, :idIndicateur)", nativeQuery = true)
       void lierIndicateur(@Param("idActivite") Integer idActivite,
                     @Param("idIndicateur") Integer idIndicateur);

       @Modifying
       @Query(value = "INSERT INTO activite_origine (id_activite, id_origine) "
                     + "VALUES (:idActivite, :idOrigine)", nativeQuery = true)
       void lierOrigine(@Param("idActivite") Integer idActivite,
                     @Param("idOrigine") Integer idOrigine);

       @Modifying
       @Query(value = """
                     DELETE FROM activite_indicateur
                     WHERE id_activite = :idActivite
                       AND id_indicateur = :idIndicateur
                     """, nativeQuery = true)
       int delierIndicateur(@Param("idActivite") Integer idActivite,
                     @Param("idIndicateur") Integer idIndicateur);

       @Query(value = """
                     SELECT COUNT(*) FROM activite_indicateur
                     WHERE id_activite = :idActivite
                       AND id_indicateur = :idIndicateur
                     """, nativeQuery = true)
       long existsLiaison(@Param("idActivite") Integer idActivite,
                     @Param("idIndicateur") Integer idIndicateur);

       @Query(value = """
                     SELECT a.id_activite       AS id,
                            a.code              AS code,
                            a.designation       AS designation,
                            a.reference         AS reference,
                            a.date_debut_prevue AS dateDebutPrevue,
                            a.date_fin_prevue   AS dateFinPrevue,
                            a.date_debut_reelle AS dateDebutReelle,
                            a.date_fin_reelle   AS dateFinReelle,
                            aa.date_affectation AS dateAffectation,
                            aa.date_desaffectation AS dateDesaffectation,
                            r.code              AS roleCode,
                            r.designation       AS roleLibelle,
                            sta.code            AS statutCode,
                            sta.libelle         AS statutLibelle,
                            COALESCE(av.avancement, 0) AS avancement
                     FROM affectation_activite aa
                     JOIN role r     ON r.id_role = aa.id_role
                     JOIN activite a ON a.id_activite = aa.id_activite
                     LEFT JOIN LATERAL (
                         SELECT h.id_statut
                         FROM historique_activite h
                         WHERE h.id_activite = a.id_activite
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
                         WHERE sa.id_activite = a.id_activite
                     ) av ON TRUE
                     WHERE aa.id_utilisateur = :idUtilisateur
                       AND aa.date_desaffectation IS NULL
                       AND a.date_debut_prevue <= CAST(:finAnnee AS DATE)
                       AND (a.date_fin_prevue IS NULL
                            OR a.date_fin_prevue >= CAST(:debutAnnee AS DATE))
                     ORDER BY
                         CASE
                             WHEN sta.code = 'EN_RETARD' THEN 0
                             WHEN a.date_fin_prevue IS NOT NULL
                                  AND a.date_fin_prevue < CURRENT_DATE
                                  AND sta.code NOT IN ('TERMINEE', 'ANNULEE', 'REJETEE') THEN 0
                             WHEN sta.code = 'EN_COURS' THEN 1
                             WHEN sta.code = 'NON_COMMENCEE' THEN 2
                             WHEN sta.code = 'TERMINEE' THEN 3
                             ELSE 4
                         END,
                         a.date_debut_prevue DESC,
                         a.id_activite DESC
                     """, nativeQuery = true)
       List<UtilisateurActiviteAnneeRow> findActivitesAnneePourUtilisateur(
                     @Param("idUtilisateur") Integer idUtilisateur,
                     @Param("debutAnnee") java.time.LocalDate debutAnnee,
                     @Param("finAnnee") java.time.LocalDate finAnnee);

       Optional<Activite> findByReferenceIgnoreCase(String reference);

       @Query(value = """
                     SELECT COUNT(*) FROM activite_origine
                     WHERE id_activite = :idActivite AND id_origine = :idOrigine
                     """, nativeQuery = true)
       long existsLiaisonOrigine(@Param("idActivite") Integer idActivite,
                     @Param("idOrigine") Integer idOrigine);

       @Query(value = """
                     SELECT COUNT(*) FROM sous_activite sa
                     WHERE sa.id_activite = :idActivite
                       AND NOT EXISTS (
                           SELECT 1 FROM avancement_sous_activite av
                           WHERE av.id_sous_activite = sa.id_sous_activite
                             AND av.valeur_pourcentage = 100
                             AND NOT EXISTS (
                                 SELECT 1 FROM avancement_sous_activite av2
                                 WHERE av2.id_sous_activite = av.id_sous_activite
                                   AND (av2.date_changement, av2.id_historique_sous_activite)
                                       > (av.date_changement, av.id_historique_sous_activite))
                       )
                     """, nativeQuery = true)
       long compterSousActivitesNonTerminees(@Param("idActivite") Integer idActivite);

       // ------------------------------------------------------------------
       // Listes du nouveau workflow
       // ------------------------------------------------------------------

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
                     + SELECT_BASE + FILTRES
                     + """
                                   AND sta.code = 'NON_COMMENCEE'
                                   AND EXISTS (
                                          SELECT 1 FROM affectation_activite aa
                                          JOIN role r ON r.id_role = aa.id_role
                                          WHERE aa.id_activite = act.id_activite
                                                 AND r.code = 'RESPONSABLE'
                                                 AND aa.date_desaffectation IS NULL
                                   )
                                                 """, countQuery = "SELECT COUNT(*) " + SELECT_BASE_COUNT + FILTRES
                                   + " AND sta.code = 'NON_COMMENCEE'"
                                   + " AND EXISTS (SELECT 1 FROM affectation_activite aa JOIN role r ON r.id_role = aa.id_role WHERE aa.id_activite = act.id_activite AND r.code = 'RESPONSABLE' AND aa.date_desaffectation IS NULL)", nativeQuery = true)
       Page<ActiviteListRow> findActivitesADemarrer(
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
                     Pageable pageable);

       String SELECT_BASE_EXPORT = """
                     FROM activite act
                     LEFT JOIN service srv
                            ON srv.id_service = act.id_service
                     LEFT JOIN objectif_specifique os
                            ON os.id_objectif_specifique = act.id_objectif_specifique
                     LEFT JOIN type_activite ta
                            ON ta.id_type_activite = act.id_type_activite
                     LEFT JOIN site st
                            ON st.id_site = act.id_site
                     LEFT JOIN priorite pr
                            ON pr.id_priorite = act.id_priorite
                     LEFT JOIN "procedure" proc
                            ON proc.id_procedure = act.id_procedure
                     """
                     + SqlFragments.STATUT_COURANT
                     + SqlFragments.AVANCEMENT_ACTIVITE_COURANT;

       @Query(value = """
                     SELECT act.id_activite                       AS id,
                            act.code                              AS code,
                            act.reference                         AS reference,
                            act.designation                       AS designation,
                            (act.id_objectif_specifique IS NOT NULL) AS pta,
                            os.code                               AS objectifCode,
                            os.designation                        AS objectifDesignation,
                            proc.designation                      AS procedureLibelle,
                            act.date_debut_prevue                 AS dateDebutPrevue,
                            act.date_fin_prevue                   AS dateFinPrevue,
                            sta.code                              AS statutCode,
                            sta.libelle                           AS statutLibelle,
                            srv.id_service                        AS serviceId,
                            srv.nom                               AS serviceNom,
                            pr.libelle                            AS prioriteLibelle,
                            COALESCE(av.avancement, 0)            AS avancement
                     """
                     + SELECT_BASE_EXPORT
                     + FILTRES
                     + """
                                   ORDER BY srv.nom, act.code
                                   """, nativeQuery = true)
       List<ActiviteExportRow> findActivitesPourExport(
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
                     @Param("dateFin") java.time.LocalDate dateFin);

       @Query(value = """
                     SELECT sa.id_activite          AS idActivite,
                            sa.id_sous_activite     AS idSousActivite,
                            sa.code                 AS code,
                            sa.designation          AS designation,
                            sa.date_debut_prevue    AS dateDebutPrevue,
                            sa.date_fin_prevue      AS dateFinPrevue,
                            sta.code                AS statutCode,
                            sta.libelle             AS statutLibelle,
                            COALESCE(dernier.valeur_pourcentage, 0) AS avancement
                     FROM sous_activite sa
                     LEFT JOIN LATERAL (
                         SELECT av.valeur_pourcentage, av.id_statut
                         FROM avancement_sous_activite av
                         WHERE av.id_sous_activite = sa.id_sous_activite
                           AND NOT EXISTS (
                               SELECT 1 FROM avancement_sous_activite av2
                               WHERE av2.id_sous_activite = av.id_sous_activite
                                 AND (av2.date_changement, av2.id_historique_sous_activite)
                                     > (av.date_changement, av.id_historique_sous_activite))
                     ) dernier ON TRUE
                     LEFT JOIN statut sta
                            ON sta.id_statut = dernier.id_statut
                     WHERE sa.id_activite IN (:ids)
                     ORDER BY sa.id_activite, sa.date_debut_prevue, sa.code
                     """, nativeQuery = true)
       List<SousActiviteExportRow> findSousActivitesPourExport(@Param("ids") List<Integer> ids);

       @Query(value = """
                     SELECT sa.id_activite              AS idActivite,
                            av.id_sous_activite         AS idSousActivite,
                            sa.code                     AS codeSousActivite,
                            sa.designation              AS designationSousActivite,
                            av.date_changement          AS dateChangement,
                            u.nom                       AS utilisateurNom,
                            u.prenom                    AS utilisateurPrenom,
                            av.valeur_pourcentage::float AS valeurPourcentage,
                            av.commentaire              AS commentaire,
                            rep.motif                   AS motif,
                            sta.code                    AS statutCode,
                            sta.libelle                 AS statutLibelle
                     FROM avancement_sous_activite av
                     JOIN sous_activite sa
                            ON sa.id_sous_activite = av.id_sous_activite
                     LEFT JOIN utilisateur u
                            ON u.id_utilisateur = av.id_utilisateur
                     LEFT JOIN statut sta
                            ON sta.id_statut = av.id_statut
                     LEFT JOIN LATERAL (
                         SELECT dr.motif
                         FROM demande_reprise_activite dr
                         WHERE dr.id_activite = sa.id_activite
                         ORDER BY dr.date_demande DESC, dr.id_demande_reprise_activite DESC
                         LIMIT 1
                     ) rep ON TRUE
                     WHERE sa.id_activite IN (:ids)
                     ORDER BY sa.id_activite, av.id_sous_activite,
                              av.date_changement DESC, av.id_historique_sous_activite DESC
                     """, nativeQuery = true)
       List<SuiviExportRow> findSuivisPourExport(@Param("ids") List<Integer> ids);

       @Query(value = """
                     SELECT DISTINCT aa.id_activite      AS idActivite,
                            CAST(NULL AS INTEGER)        AS idSousActivite,
                            u.id_utilisateur             AS idUtilisateur,
                            u.nom                        AS nom,
                            u.prenom                     AS prenom
                     FROM affectation_activite aa
                     JOIN utilisateur u
                            ON u.id_utilisateur = aa.id_utilisateur
                     WHERE aa.id_activite IN (:ids)
                       AND aa.date_desaffectation IS NULL
                     ORDER BY aa.id_activite, u.nom, u.prenom
                     """, nativeQuery = true)
       List<ResponsableExportRow> findResponsablesActivitesPourExport(@Param("ids") List<Integer> ids);

       @Query(value = """
                     SELECT DISTINCT sa.id_activite        AS idActivite,
                            sa.id_sous_activite           AS idSousActivite,
                            u.id_utilisateur              AS idUtilisateur,
                            u.nom                         AS nom,
                            u.prenom                      AS prenom
                     FROM sous_activite sa
                     JOIN affectation_sous_activite aff
                            ON aff.id_sous_activite = sa.id_sous_activite
                     JOIN utilisateur u
                            ON u.id_utilisateur = aff.id_utilisateur
                     WHERE sa.id_activite IN (:ids)
                       AND aff.date_desaffectation IS NULL
                     ORDER BY sa.id_activite, sa.id_sous_activite, u.nom, u.prenom
                     """, nativeQuery = true)
       List<ResponsableExportRow> findResponsablesSousActivitesPourExport(@Param("ids") List<Integer> ids);

       @Modifying
       @Query(value = """
                     DELETE FROM fichier_sous_activite
                     WHERE id_livrable_sous_activite IN (
                         SELECT l.id_livrable_sous_activite FROM livrable_sous_activite l
                         JOIN sous_activite sa ON sa.id_sous_activite = l.id_sous_activite
                         WHERE sa.id_activite = :id
                     )
                     """, nativeQuery = true)
       void supprimerFichiersEnfants(@Param("id") Integer id);

       @Modifying
       @Query(value = """
                     DELETE FROM livrable_sous_activite
                     WHERE id_sous_activite IN (
                         SELECT id_sous_activite FROM sous_activite WHERE id_activite = :id
                     )
                     """, nativeQuery = true)
       void supprimerLivrablesEnfants(@Param("id") Integer id);

       @Modifying
       @Query(value = """
                     DELETE FROM affectation_sous_activite
                     WHERE id_sous_activite IN (
                         SELECT id_sous_activite FROM sous_activite WHERE id_activite = :id
                     )
                     """, nativeQuery = true)
       void supprimerAffectationsEnfants(@Param("id") Integer id);

       @Modifying
       @Query(value = """
                     DELETE FROM avancement_sous_activite
                     WHERE id_sous_activite IN (
                         SELECT id_sous_activite FROM sous_activite WHERE id_activite = :id
                     )
                     """, nativeQuery = true)
       void supprimerAvancementsEnfants(@Param("id") Integer id);

       @Modifying
       @Query("DELETE FROM SousActivite sa WHERE sa.activite.idActivite = :id")
       void supprimerSousActivites(@Param("id") Integer id);

       @Modifying
       @Query("DELETE FROM ResultatIntermediaire r WHERE r.activite.idActivite = :id")
       void supprimerResultats(@Param("id") Integer id);

       @Modifying
       @Query("DELETE FROM HistoriqueActivite h WHERE h.activite.idActivite = :id")
       void supprimerHistorique(@Param("id") Integer id);

       @Modifying
       @Query(value = "DELETE FROM validation_activite WHERE id_activite = :id", nativeQuery = true)
       void supprimerValidations(@Param("id") Integer id);

       @Modifying
       @Query(value = "DELETE FROM activite_indicateur WHERE id_activite = :id", nativeQuery = true)
       void supprimerLiaisonIndicateurs(@Param("id") Integer id);

       @Modifying
       @Query(value = "DELETE FROM activite_origine WHERE id_activite = :id", nativeQuery = true)
       void supprimerLiaisonOrigines(@Param("id") Integer id);

       @Modifying
       @Query("DELETE FROM Activite a WHERE a.idActivite = :id")
       void supprimerActiviteParId(@Param("id") Integer id);

       default void supprimerCascade(Integer id) {
              supprimerFichiersEnfants(id);
              supprimerLivrablesEnfants(id);
              supprimerAffectationsEnfants(id);
              supprimerAvancementsEnfants(id);
              supprimerSousActivites(id);
              supprimerResultats(id);
              supprimerHistorique(id);
              supprimerValidations(id);
              supprimerLiaisonIndicateurs(id);
              supprimerLiaisonOrigines(id);
              supprimerActiviteParId(id);
       }
}
