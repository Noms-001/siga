package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
import mg.bank.backend.repository.projection.ResultatIntermediaireRow;
import mg.bank.backend.repository.projection.SousActiviteDetailRow;
import mg.bank.backend.repository.projection.ValidationActiviteRow;
import mg.bank.backend.repository.projection.ValeurIndicateurRow;

public interface ActiviteRepository extends JpaRepository<Activite, Integer> {

    /**
     * Codes de statut pretats pour le SQL : la constante Java entouree de
     * guillemets simples.
     *
     * Ils doivent etre concatenes HORS d un bloc de texte : dans un bloc
     * """...""" le + reste un caractere litteral, et la comparaison
     * porterait sur la chaine '..." + ... + "...' au lieu du code.
     */
    String SQL_EN_COURS = "'" + StatutsActivite.EN_COURS + "'";
    String SQL_TERMINEE = "'" + StatutsActivite.TERMINEE + "'";
    String SQL_ANNULEE = "'" + StatutsActivite.ANNULEE + "'";
    String SQL_REPORTEE = "'" + StatutsActivite.REPORTEE + "'";
    String SQL_EN_RETARD = "'" + StatutsActivite.EN_RETARD + "'";
    String SQL_NON_COMMENCEE = "'" + StatutsActivite.NON_COMMENCEE + "'";
    String SQL_SUSPENDUE = "'" + StatutsActivite.SUSPENDUE + "'";

    /**
     * Codes des statuts invisibles dans le suivi, SANS parentheses.
     *
     * Une activite encore dans le circuit de validation n est pas publiee :
     * la montrer dans une liste de suivi exposerait un travail en cours de
     * validation. La regle porte sur le STATUT COURANT, pas sur l historique,
     * et elle s applique aussi aux cards, sinon le total afficherait des
     * activites absentes du tableau.
     *
     * Le nom "non publiees" est choisi plutot qu "exclues" : il dit ce que
     * la regle EST, pas ce qu elle fait.
     *
     * Les parentheses sont mises par l appelant, et c est volontaire : si
     * elles etaient ici, un appelant qui ajoute les siennes ecrit
     * NOT IN ((...)), que PostgreSQL lit comme une comparaison
     * varchar = record et refuse ("operator does not exist").
     */
    String SQL_NON_PUBLIEES =
            "'" + StatutsActivite.EN_ATTENTE_VALIDATION + "', '"
                    + StatutsActivite.VALIDEE + "', '"
                    + StatutsActivite.REJETE + "'";

    /**
     * Colonnes et jointures communes a la liste et aux statistiques.
     *
     * L'activite ne porte ni son statut ni sa date de creation : les deux
     * sont reconstruits depuis historique_activite, par LATERAL.
     *
     * REGLE ABSOLUE pour toute requete paginee de ce fichier : aucun
     * "ORDER BY" dans ce SQL, ni dans ce bloc ni dans aucun autre. Le
     * ORDER BY de la liste est produit par le Pageable, et Spring Data ne
     * l injecte que si hasOrderByClause() renvoie false. Cette methode
     * compte les "order by" du texte mais ne retire qu UNE occurrence de
     * sous-requete (regex gloutonne), donc 2 ORDER BY dans des LATERAL
     * suffisent a la tromper : elle concatene alors le tri sans le mot-cle
     * et PostgreSQL repond "syntax error at or near ','".
     *
     * Cette regle ne concerne pas autocomplete, qui ne recoit pas de
     * Pageable et n appelle donc jamais hasOrderByClause.
     *
     * La derniere ligne de chaque table est donc designee par un NOT EXISTS a
     * comparaison de tuples, ce qui est exactement equivalent a
     * "ORDER BY date_changement DESC, id DESC LIMIT 1" : la ligne retenue est
     * celle pour laquelle aucune ligne plus recente n existe.
     */
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

    /**
     * Filtres, tous facultatifs.
     *
     * Chaque contrainte s'active uniquement si le parametre est non nul.
     * Les valeurs nulles signifient donc "pas de contrainte" et non
     * "egal a NULL", d'ou le motif (:p IS NULL OR ...).
     *
     * La regle metier reste dans le service : c'est lui qui decide
     * quels parametres il transmet (voir ActiviteService).
     *
     * REGLE ABSOLUE pour tout @Query natif de ce projet : aucune
     * apostrophe, ni dans un litteral ni dans un commentaire "--".
     * Spring Data analyse la chaine pour distinguer SQL et litteraux, et
     * bascule un booleen a chaque apostrophe : la premiere fait basculer tout
     * le reste en "chaine", et les parametres nommes qui suivent cessent
     * d etre enregistres. Symptome : QueryParameterException sur un
     * parametre qui est bien present dans la methode.
     */
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

            AND (:statutCode IS NULL
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
            ))

            AND (:annee IS NULL
                 OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
            AND (:moisDebut IS NULL
                 OR EXTRACT(MONTH FROM act.date_debut_prevue) BETWEEN :moisDebut AND :moisFin)

            AND (CAST(:dateDebut AS DATE) IS NULL
                 OR act.date_debut_prevue >= CAST(:dateDebut AS DATE))
            AND (CAST(:dateFin AS DATE) IS NULL
                 OR act.date_fin_prevue <= CAST(:dateFin AS DATE))
            """;

    /**
     * Liste paginee et filtree.
     *
     * Le ORDER BY n'est pas ecrit ici : il vient du Pageable, et le service
     * impose par defaut la recurrence decroissante. La colonne de tri
     * dateCreationTri n'est jamais nulle, donc aucune clause NULLS LAST
     * n'est necessaire.
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
            + SELECT_BASE
            + FILTRES,
            countQuery = "SELECT COUNT(*) " + SELECT_BASE + FILTRES,
            nativeQuery = true)
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

    /**
     * Compteurs des cards.
     *
     * Le parametre de statut n'est PAS pris en compte ici, volontairement :
     * si les compteurs etaient filtres par statut, cliquer sur une card
     * mettrait toutes les autres a zero et la navigation serait perdue.
     * Les cards montrent donc la repartition des autres filtres filtres, et
     * la card active est signalee au front par le filtre qu elle pose.
     */
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
            + FILTRES,
            nativeQuery = true)
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

    /**
     * Autocomplete activite : code, reference ou designation.
     * Deja restreint au perimetre du demandeur, et aux activites publiees.
     *
     * Le ORDER BY act.code est sans danger ici : cette requete ne recoit pas
     * de Pageable, donc Spring Data n injecte aucun tri et n appelle pas
     * hasOrderByClause.
     */
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
            """,
            nativeQuery = true)
    List<OptionRow> autocomplete(
            @Param("recherche") String recherche,
            @Param("motif") String motif,
            @Param("servicePerimetre") Integer servicePerimetre,
            @Param("departementPerimetre") Integer departementPerimetre,
            @Param("limite") int limite);

    // ------------------------------------------------------------------
    // Detail
    // ------------------------------------------------------------------

    /**
     * Detail d UNE activite, dans le perimetre de l appelant.
     *
     * Meme construction que la liste : SELECT_BASE est reutilise tel quel, de
     * sorte que le statut courant, la date de creation et l avancement affiches
     * dans le detail ne puissent pas diverger de ceux affiches dans la liste.
     * Les deux lectures ne partagent que leur source, pas leur SQL.
     *
     * Le perimetre est applique ICI et non apres coup dans le service : une
     * ligne qui ne correspond pas n est jamais chargee, donc jamais presente
     * en memoire. C est ce qui permet de distinguer un identifiant absent d un
     * identifiant interdit.
     *
     * Cette requete ne recoit pas de Pageable : elle porte un seul
     * identifiant, donc aucun ORDER BY n est necessaire, et la regle des
     * LATERAL sans ORDER BY ne s applique pas.
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
            """,
            nativeQuery = true)
    Optional<ActiviteDetailRow> findActiviteDetail(
            @Param("id") Integer id,
            @Param("servicePerimetre") Integer servicePerimetre,
            @Param("departementPerimetre") Integer departementPerimetre,
            @Param("codesTerminaux") String codesTerminaux);

    /**
     * L activite existe-t-elle pour cet appelant, meme si le detail n a pas pu
     * etre lu ?
     *
     * C est ce qui permet de repondre 403 plutot que 404 lorsqu une activite
     * existe mais appartient a un autre perimetre : sans cette verification,
     * l appelant ne verrait que 404, ce qui ferait croire a une faute de
     * frappe alors que l activite est ailleurs.
     *
     * Elle n estPAS executee dans le cas nominal. Le service ne l appelle
     * qu apres un echec de findActiviteDetail, donc uniquement sur un acces
     * refuse : le cout n est paye que lorsqu il ya a quelque chose a signaler.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM activite act
            LEFT JOIN service srv
                   ON srv.id_service = act.id_service
            WHERE act.id_activite = :id
            """,
            nativeQuery = true)
    long compterActivite(@Param("id") Integer id);

    /**
     * Sous-activites d une activite.
     *
     * Tri par date de debut puis par code : un ordre stable, independant de
     * l ordre physique des lignes. Le code en second critere tranche les
     * sous-activites qui commenceraient le meme jour.
     */
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
            """,
            nativeQuery = true)
    List<SousActiviteDetailRow> findSousActivites(@Param("id") Integer id);

    /**
     * Tous les releves d avancement des sous-activites d une activite.
     *
     * UNE seule requete pour toutes les sous-activites : c est la condition
     * pour que le nombre de requetes ne depende pas du nombre de
     * sous-activites. Le ORDER BY commence par la sous-activite, ce qui
     * permet de regrouper les lignes en memoire, puis trie chaque groupe du
     * plus recent au plus ancien : la premiere ligne de chaque groupe est
     * donc l avancement courant.
     */
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
            """,
            nativeQuery = true)
    List<AvancementDetailRow> findAvancements(@Param("id") Integer id);

    /**
     * Affectations de toutes les sous-activites d une activite, en une requete.
     *
     * Les plus recentes d abord : une affectation close reste dans la liste
     * pour l historique des responsables, et c est la plus recente qui
     * interesse en premier.
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
            JOIN sous_activite sa
                   ON sa.id_sous_activite = a.id_sous_activite
            LEFT JOIN role r
                   ON r.id_role = a.id_role
            LEFT JOIN utilisateur u
                   ON u.id_utilisateur = a.id_utilisateur
            WHERE sa.id_activite = :id
            ORDER BY sa.id_sous_activite, a.date_affectation DESC
            """,
            nativeQuery = true)
    List<AffectationDetailRow> findAffectations(@Param("id") Integer id);

    /**
     * Livrables de toutes les sous-activites d une activite, en une requete.
     */
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
            """,
            nativeQuery = true)
    List<LivrableDetailRow> findLivrables(@Param("id") Integer id);

    /**
     * Fichiers de tous les livrables de toutes les sous-activites d une
     * activite, en une requete.
     *
     * chemin_fichier n est volontairement pas selectionne : c est un chemin de
     * stockage interne, sans usage tant qu aucun telechargement n existe.
     */
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
            """,
            nativeQuery = true)
    List<FichierDetailRow> findFichiers(@Param("id") Integer id);

    /**
     * Passages de l activite par les etapes de son circuit de validation.
     *
     * Tri par niveau d etape croissant : c est l ordre du circuit de
     * validation, du premier au dernier niveau, et non un ordre de
     * consultation. La date de demande ne sert qu a ordonner plusieurs
     * passages de meme niveau.
     *
     * Le nom de la procedure est joint pour qu une etape soit identifiable :
     * la designation seule ne distingue pas deux procedures qui auraient des
     * etapes de meme nom.
     */
    @Query(value = """
            SELECT v.id_validation_activite  AS id,
                   v.decision                 AS decision,
                   v.commentaire              AS commentaire,
                   v.date_demande             AS dateDemande,
                   v.date_decision            AS dateDecision,
                   e.id_etape_validation      AS etapeId,
                   e.designation              AS etapeDesignation,
                   e.description              AS etapeDescription,
                   e.niveau                   AS etapeNiveau,
                   e.obligatoire              AS etapeObligatoire,
                   pr.designation             AS etapeProcedureLibelle,
                   d.id_utilisateur           AS demandeurId,
                   d.nom                      AS demandeurNom,
                   d.prenom                   AS demandeurPrenom,
                   dc.id_utilisateur          AS decideurId,
                   dc.nom                     AS decideurNom,
                   dc.prenom                  AS decideurPrenom
            FROM validation_activite v
            JOIN etape_validation e
                   ON e.id_etape_validation = v.id_etape_validation
            LEFT JOIN procedure pr
                   ON pr.id_procedure = e.id_procedure
            LEFT JOIN utilisateur d
                   ON d.id_utilisateur = v.id_demandeur
            LEFT JOIN utilisateur dc
                   ON dc.id_utilisateur = v.id_decideur
            WHERE v.id_activite = :id
            ORDER BY e.niveau, v.date_demande, v.id_validation_activite
            """,
            nativeQuery = true)
    List<ValidationActiviteRow> findValidations(@Param("id") Integer id);

    /**
     * Indicateurs rattaches a l activite, tries par code.
     *
     * activite_indicateur est une table de jointure pure, sans colonne
     * propre : il n existe donc aucun indicateur propre a cette activite, et
     * les mesures partagees par plusieurs activites apparaissent chez
     * chacune d entre elles.
     */
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
            """,
            nativeQuery = true)
    List<IndicateurDetailRow> findIndicateurs(@Param("id") Integer id);

    /**
     * Mesures de tous les indicateurs de l activite, en une requete.
     *
     * Comme pour les avancements, le tri groupe d abord par indicateur puis
     * classe chaque groupe par periode decroissante : la premiere ligne d un
     * groupe est la valeur courante.
     */
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
            """,
            nativeQuery = true)
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
            """,
            nativeQuery = true)
    List<ResultatIntermediaireRow> findResultatsIntermediaires(@Param("id") Integer id);

    /**
     * Historique de statut de l activite, du plus recent au plus ancien.
     *
     * Le second critere de tri n est pas decoratif : plusieurs changements
     * peuvent partager la meme date_changement, et c est alors la plus grande
     * cle qui fixe l ordre. Sans lui, deux lignes de meme date pourraient
     * changer de place d une requete a l autre, et la premiere ligne -- celle
     * qui porte le statut courant -- ne serait pas stable.
     */
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
            """,
            nativeQuery = true)
    List<HistoriqueActiviteRow> findHistorique(@Param("id") Integer id);

    // ------------------------------------------------------------------
    // Detail d une sous-activite
    // ------------------------------------------------------------------

    /**
     * Sous-activite precise, dans le perimetre de l appelant.
     *
     * Le perimetre porte sur l activite parente : il est joint ici pour que
     * soit refusees (403) les sous-activites d une activite hors perimetre,
     * avec la meme distinction 403/404 que le detail d activite.
     */
    @Query(value = """
            SELECT sa.id_sous_activite  AS id,
                   sa.code              AS code,
                   sa.designation       AS designation,
                   sa.date_debut_prevue AS dateDebutPrevue,
                   sa.date_fin_prevue   AS dateFinPrevue,
                   sa.date_debut_reelle AS dateDebutReelle,
                   sa.date_fin_reelle   AS dateFinReelle
            FROM sous_activite sa
            JOIN activite act
                   ON act.id_activite = sa.id_activite
            LEFT JOIN service srv
                   ON srv.id_service = act.id_service
            WHERE sa.id_sous_activite = :idSousActivite
              AND act.id_activite = :idActivite
              AND (:servicePerimetre IS NULL
                   OR act.id_service = :servicePerimetre)
              AND (:departementPerimetre IS NULL
                   OR srv.id_departement = :departementPerimetre)
            """,
            nativeQuery = true)
    Optional<SousActiviteDetailRow> findSousActivite(
            @Param("idActivite") Integer idActivite,
            @Param("idSousActivite") Integer idSousActivite,
            @Param("servicePerimetre") Integer servicePerimetre,
            @Param("departementPerimetre") Integer departementPerimetre);

    /**
     * La sous-activite existe-t-elle, quel que soit son rattachement ?
     *
     * Executee seulement sur le chemin d erreur, apres l echec de
     * findSousActivite : elle distingue une sous-activite inexistante d une
     * sous-activite interdite, sans payer ce cout en fonctionnement normal.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM sous_activite
            WHERE id_sous_activite = :id
            """,
            nativeQuery = true)
    long compterSousActivite(@Param("id") Integer id);

    /**
     * La sous-activite est-elle rattachee a cette activite ?
     *
     * Sans filtre de perimetre : le rattachement est une propriete de la
     * donnee, pas de l appelant. Une sous-activite d une autre activite est
     * une ressource inexistante dans le parent demande, donc un 404, pas un
     * 403.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM sous_activite
            WHERE id_sous_activite = :idSousActivite
              AND id_activite = :idActivite
            """,
            nativeQuery = true)
    long compterSousActiviteDeActivite(
            @Param("idActivite") Integer idActivite,
            @Param("idSousActivite") Integer idSousActivite);

    /**
     * Releves d avancement d UNE sous-activite, du plus recent au plus ancien.
     *
     * C est le pendant de findAvancements, restreint a une sous-activite :
     * la premiere ligne porte le releve courant, les suivantes le suivi. Le
     * second critere de tri tranche les releves partageant une meme date.
     */
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
            """,
            nativeQuery = true)
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
            """,
            nativeQuery = true)
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
            """,
            nativeQuery = true)
    List<LivrableDetailRow> findLivrablesSousActivite(@Param("id") Integer id);

    /**
     * Fichiers de tous les livrables d UNE sous-activite, en une requete.
     *
     * Memes colonnes que findFichiers, sans chemin_fichier, et meme tri :
     * groupe par livrable, puis par nom d origine et version decroissante.
     */
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
            """,
            nativeQuery = true)
    List<FichierDetailRow> findFichiersSousActivite(@Param("id") Integer id);

    // ------------------------------------------------------------------
    // Telechargement d un fichier
    // ------------------------------------------------------------------

    /**
     * Fichier precise, au bout de la chaine livrable -> sous-activite ->
     * activite, dans le perimetre de l appelant.
     *
     * Meme distinction 403/404 que le detail : une ligne absente ici peut
     * etre un fichier inconnu, un fichier d une autre sous-activite, ou un
     * fichier d une activite hors perimetre. Le service departage ensuite
     * sur le chemin d erreur.
     */
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
            """,
            nativeQuery = true)
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
            """,
            nativeQuery = true)
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
            """,
            nativeQuery = true)
    long compterFichierDeSousActivite(
            @Param("idSousActivite") Integer idSousActivite,
            @Param("idFichier") Integer idFichier);

}
