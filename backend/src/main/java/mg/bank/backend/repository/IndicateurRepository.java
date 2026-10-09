package mg.bank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.Indicateur;

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
}