package mg.bank.backend.repository.indicateur;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.indicateur.Indicateur;

public interface IndicateurRepository extends JpaRepository<Indicateur, Integer> {

        /* -------------------- lecture -------------------- */

        List<Indicateur> findByActifTrue();

        /**
         * Filtre optionnel par actif, type et recherche.
         *
         * CHAQUE PARAMÈTRE NULLABLE EST CASTÉ EXPLICITEMENT
         *
         * Sans le CAST, Hibernate ne peut pas inférer le type d'un paramètre null
         * et le transmet à PostgreSQL en `bytea`. `lower()` et `like()` refusent
         * alors la requête — "function lower(bytea) does not exist". Le cast dit à
         * Hibernate sous quel type envoyer la valeur, qu'elle soit null ou non.
         *
         * Le CAST entoure l'usage réel (`LOWER(CAST(:motif AS String))`) et non la
         * comparaison IS NULL : c'est l'appel à une fonction typée qui exige que
         * Hibernate connaisse le type, pas le test d'absence de valeur.
         */
        @Query("""
                        SELECT i FROM Indicateur i
                        WHERE (CAST(:actif AS Boolean) IS NULL OR i.actif = :actif)
                          AND (CAST(:type AS String) IS NULL OR i.typeIndicateur = :type)
                          AND (CAST(:motif AS String) IS NULL
                               OR LOWER(i.code) LIKE LOWER(CAST(:motif AS String))
                               OR LOWER(i.designation) LIKE LOWER(CAST(:motif AS String)))
                        ORDER BY i.code
                        """)
        List<Indicateur> rechercher(
                        @Param("actif") Boolean actif,
                        @Param("type") String type,
                        @Param("motif") String motif);

        /**
         * Valeurs distinctes de type_indicateur, pour le filtre.
         * Triées, ignore les null.
         */
        @Query("SELECT DISTINCT i.typeIndicateur FROM Indicateur i "
                        + "WHERE i.typeIndicateur IS NOT NULL ORDER BY i.typeIndicateur")
        List<String> findDistinctTypes();

        /* -------------------- unicités -------------------- */

        Optional<Indicateur> findByCode(String code);

        boolean existsByCode(String code);

        boolean existsByCodeAndIdIndicateurNot(String code, Integer id);

        /* -------------------- unicités conditionnelles -------------------- */

        boolean existsByCodeHopex(String codeHopex);

        boolean existsByCodeHopexAndIdIndicateurNot(String codeHopex, Integer id);

        boolean existsByIndicateurHopex(String indicateurHopex);

        boolean existsByIndicateurHopexAndIdIndicateurNot(String indicateurHopex, Integer id);

        long countByDesignationIgnoreCase(String designation);

        Optional<Indicateur> findByDesignationIgnoreCase(String designation);

        boolean existsByCodeIgnoreCase(String code);

        // Vérifie qu'un indicateur est lié à au moins une activité dans le périmètre.
        @Query(value = """
                        SELECT COUNT(*)
                        FROM activite_indicateur ai
                        JOIN activite act ON act.id_activite = ai.id_activite
                        JOIN service srv ON srv.id_service = act.id_service
                        WHERE ai.id_indicateur = :idIndicateur
                          AND (:servicePerimetre IS NULL OR act.id_service = :servicePerimetre)
                          AND (:departementPerimetre IS NULL OR srv.id_departement = :departementPerimetre)
                        """, nativeQuery = true)
        long compterDansPerimetre(
                        @Param("idIndicateur") Integer idIndicateur,
                        @Param("servicePerimetre") Integer servicePerimetre,
                        @Param("departementPerimetre") Integer departementPerimetre);

        /**
         * Indicateurs actifs, avec frequence_verification renseignee, visibles
         * dans le perimetre de l'appelant (via activite_indicateur).
         */
        @Query(value = """
                        SELECT DISTINCT i.*
                        FROM indicateur i
                        JOIN activite_indicateur ai ON ai.id_indicateur = i.id_indicateur
                        JOIN activite act ON act.id_activite = ai.id_activite
                        JOIN service srv ON srv.id_service = act.id_service
                        WHERE i.actif = TRUE
                          AND i.frequence_verification IS NOT NULL
                          AND (:servicePerimetre IS NULL OR act.id_service = :servicePerimetre)
                          AND (:departementPerimetre IS NULL OR srv.id_departement = :departementPerimetre)
                        ORDER BY i.code
                        """, nativeQuery = true)
        List<Indicateur> findActifsAvecFrequenceDansPerimetre(
                        @Param("servicePerimetre") Integer servicePerimetre,
                        @Param("departementPerimetre") Integer departementPerimetre);

        /**
         * Indicateurs actifs non encore liés à une activité, pour la modale
         * "ajouter un indicateur à cette activité". Filtre optionnel par code
         * ou désignation.
         */
        @Query(value = """
                        SELECT i.* FROM indicateur i
                        WHERE i.actif = TRUE
                          AND NOT EXISTS (
                              SELECT 1 FROM activite_indicateur ai
                              WHERE ai.id_indicateur = i.id_indicateur
                                AND ai.id_activite = :idActivite
                          )
                          AND (:motif IS NULL
                               OR LOWER(i.code) LIKE LOWER(CAST(:motif AS VARCHAR))
                               OR LOWER(i.designation) LIKE LOWER(CAST(:motif AS VARCHAR)))
                        ORDER BY i.code
                        """, nativeQuery = true)
        List<Indicateur> findDisponiblesPourActivite(
                        @Param("idActivite") Integer idActivite,
                        @Param("motif") String motif);
}