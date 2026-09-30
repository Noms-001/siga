package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.Statut;
import mg.bank.backend.repository.projection.OptionRow;

public interface StatutRepository extends JpaRepository<Statut, Integer> {

    /**
     * Statuts actifs, pour que le front n'invente aucun libelle.
     *
     * Les statuts du circuit de validation sont exclus : ils ne sont jamais
     * visibles dans le suivi, donc les proposer dans le filtre Serializable
     * n offrirait qu un choix sans aucun resultat. Ils restent lisibles par
     * l historique d une activite, qui n utilise pas cette requete.
     */
    @Query(value = """
            SELECT id_statut AS id,
                   code       AS code,
                   libelle    AS libelle
            FROM statut
            WHERE actif = TRUE
              AND code NOT IN ("""
            + ActiviteRepository.SQL_NON_PUBLIEES
            + """
            )
            ORDER BY code
            """,
            nativeQuery = true)
    List<OptionRow> findOptionsActifs();

}
