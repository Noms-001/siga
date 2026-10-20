package mg.bank.backend.repository;

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
import mg.bank.backend.model.Activite;
import mg.bank.backend.repository.projection.ActiviteDetailRow;
import mg.bank.backend.repository.projection.ActiviteListRow;
import mg.bank.backend.repository.projection.ActiviteStatistiquesRow;
import mg.bank.backend.repository.projection.AffectationDetailRow;
import mg.bank.backend.repository.projection.AvancementDetailRow;
import mg.bank.backend.repository.projection.FichierDetailRow;
import mg.bank.backend.repository.projection.FichierTelechargementRow;
import mg.bank.backend.repository.projection.HistoriqueActiviteRow;
import mg.bank.backend.repository.projection.IndicateurDetailRow;
import mg.bank.backend.repository.projection.LivrableDetailRow;
import mg.bank.backend.repository.projection.OptionRow;
import mg.bank.backend.repository.projection.ResponsableSuiviRow;
import mg.bank.backend.repository.projection.ResultatIntermediaireRow;
import mg.bank.backend.repository.projection.SousActiviteDetailRow;
import mg.bank.backend.repository.projection.SousActiviteSuiviRow;
import mg.bank.backend.repository.projection.StatutCourantRow;
import mg.bank.backend.repository.projection.ValeurIndicateurRow;

public interface ActiviteRepository extends JpaRepository<Activite, Integer> {

       String SQL_EN_COURS = "'" + StatutsActivite.EN_COURS + "'";
       String SQL_TERMINEE = "'" + StatutsActivite.TERMINEE + "'";
       String SQL_ANNULEE = "'" + StatutsActivite.ANNULEE + "'";
       String SQL_REPORTEE = "'" + StatutsActivite.REPORTEE + "'";
       String SQL_EN_RETARD = "'" + StatutsActivite.EN_RETARD + "'";
       String SQL_NON_COMMENCEE = "'" + StatutsActivite.NON_COMMENCEE + "'";
       String SQL_SUSPENDUE = "'" + StatutsActivite.SUSPENDUE + "'";

       String SQL_NON_PUBLIEES = "'" + StatutsActivite.EN_ATTENTE_VALIDATION + "', '"
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
                     LEFT JOIN LATERAL (
                         SELECT MIN(h2.date_changement) AS date_creation
                         FROM historique_activite h2
                         WHERE h2.id_activite = act.id_activite
                     ) crea ON TRUE
                     LEFT JOIN LATERAL (
                         SELECT AVG(dernier.valeur_pourcentage) AS avancement
                         FROM sous_activite sa
                         JOIN LATERAL (
                             SELECT av2.valeur_pourcentage
                             FROM avancement_sous_activite av2
                             WHERE av2.id_sous_activite = sa.id_sous_activite
                               AND NOT EXISTS (
                                   SELECT 1
                                   FROM avancement_sous_activite av3
                                   WHERE av3.id_sous_activite = sa.id_sous_activite
                                     AND (av3.date_changement, av3.id_historique_sous_activite)
                                         > (av2.date_changement, av2.id_historique_sous_activite))
                         ) dernier ON TRUE
                         WHERE sa.id_activite = act.id_activite
                     ) av ON TRUE
                     """;

       String FILTRES = """
                     WHERE 1 = 1

                     -- PTA : un objectif specifique distingue PTA et NON PTA
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

                                   AND (:annee IS NULL
                                          OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
                                   AND (:moisDebut IS NULL
                                          OR EXTRACT(MONTH FROM act.date_debut_prevue) BETWEEN :moisDebut AND :moisFin)

                                   AND (CAST(:dateDebut AS DATE) IS NULL
                                          OR act.date_debut_prevue >= CAST(:dateDebut AS DATE))
                                   AND (CAST(:dateFin AS DATE) IS NULL
                                          OR act.date_fin_prevue <= CAST(:dateFin AS DATE))
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
                     + FILTRES, countQuery = "SELECT COUNT(*) " + SELECT_BASE + FILTRES, nativeQuery = true)
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

       boolean existsByCodeIgnoreCase(String code);

       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT a FROM Activite a WHERE a.idActivite = :id")
       Optional<Activite> verrouillerActivite(@Param("id") Integer id);

       @Query("SELECT COUNT(a) FROM Activite a WHERE a.idActivite <= :id")
       int compterActivitesJusqua(@Param("id") Integer id);

       @Query(value = """
                     SELECT COALESCE(MAX(CAST(SUBSTRING(a.code FROM '^A-[0-9]{4}-([0-9]+)$')
                                           AS INTEGER)), 0)
                     FROM activite a
                     WHERE a.code ~ ('^A-' || :annee || '-\\d+$')
                     """, nativeQuery = true)
       int dernierNumeroNonPta(@Param("annee") int annee);

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
                            i.code_hopex                 AS codeHopex,
                            i.indicateur_hopex           AS indicateurHopex,
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

       /**
        * Activites en attente de decision a une etape donnee, dans l ordre d arrivee.
        *
        * REQUETE NATIVE, PAS JPQL -- ET C EST LE TYPE QUI L IMPOSE
        *
        * decision_validation est un type enum natif PostgreSQL, cree par la
        * migration. Hibernate lie un parametre @Enumerated(EnumType.STRING) en
        * varchar, et PostgreSQL n a pas d operateur = entre un enum et un varchar :
        * "operator does not exist: decision_validation = character varying".
        * Le cast explicite CAST(:decision AS decision_validation) remet les deux
        * cotes dans le meme domaine, sans changer la colonne.
        *
        * Le parametre arrive en String et non en DecisionValidation : c est ce qui
        * permet au parametre nomme de valoir NULL sans que le cast ne bute sur
        * un type enum cote Java. Le service lui passe decision.name() quand il en a
        * une, null sinon.
        *
        * PARENTHESES AUTOUR DU OR, ET PAS DECORATIF
        *
        * Sans elles, AND l emporte sur OR et la condition se lit :
        * (:decision IS NULL) OR (v.decision = :decision AND v.id_etape = :id)
        * Des que :decision est null, le filtre d etape est ignore et la requete
        * renvoie les validations de TOUTES les etapes. Le regroupement force la
        * lecture voulue : "soit on ne filtre pas par decision, soit on filtre par
        * cette decision, ET dans tous les cas on filtre par etape".
        *
        * ORDER BY v.date_demande : plus ancienne d abord, ce qui met en tete la
        * demande qui attend depuis le plus longtemps. La requete n est pas paginee,
        * donc l ORDER BY ne passe pas par hasard sous hasOrderByClause.
        */
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
                           AND h.dateChangement = (
                               SELECT MAX(h2.dateChangement)
                               FROM HistoriqueActivite h2
                               WHERE h2.activite = a
                           )
                     )
                     ORDER BY a.dateDebutPrevue ASC, a.idActivite ASC
                     """)
       List<Activite> findBrouillonsByUtilisateur(
                     @Param("codeBrouillon") String codeBrouillon,
                     @Param("idUtilisateur") Integer idUtilisateur);

       /**
        * Sous-activités de plusieurs activités en UNE requête.
        *
        * C'est ce qui évite le N+1 : sans ce batch, chaque activité de la page
        * déclencherait une requête supplémentaire. Le tri par date puis code est
        * celui de findSousActivites, pour que l'ordre affiché soit identique.
        */
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

       /**
        * Premier responsable actif par activité.
        *
        * Il n'existe pas de "responsable d'activité" en base : la règle appliquée
        * ici est celle du projet — le responsable est le premier utilisateur
        * affecté (au sens de affectation_sous_activite) sur l'une des
        * sous-activités de l'activité, tant que son affectation n'est pas close
        * (date_desaffectation IS NULL).
        *
        * DISTINCT ON (id_activite) : PostgreSQL renvoie une seule ligne par
        * activité, la plus ancienne affectation active — l'ordre du ORDER BY
        * est donc significatif et vient APRÈS le DISTINCT ON.
        *
        * Le tri date_affectation ASC fait que c'est le premier affecté qui est
        * retenu, pas le dernier. La règle pourrait être discutée : le spec ne
        * précise pas. À ajuster si "responsable actuel" doit se lire différemment.
        */
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

       /**
        * Avancement d'une activité : moyenne des derniers avancements de ses
        * sous-activités. Null si aucune sous-activité n'a de relevé.
        *
        * Le COALESCE(..., 0) est posé dans le service, pas ici : la requête dit
        * la vérité ("pas de relevé"), le service décide ce qu'il en fait.
        */
       @Query(value = """
                     SELECT AVG(dernier.valeur_pourcentage) AS avancement
                     FROM sous_activite sa
                     JOIN LATERAL (
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
}
