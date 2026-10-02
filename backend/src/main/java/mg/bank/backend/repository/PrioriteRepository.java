package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.Priorite;
import mg.bank.backend.repository.projection.OptionRow;

public interface PrioriteRepository extends JpaRepository<Priorite, Integer> {

    /**
     * Priorites actives, pour le filtre. Aucune valeur codee en dur.
     */
    @Query(value = """
            SELECT id_priorite AS id,
                   code        AS code,
                   libelle     AS libelle
            FROM priorite
            WHERE actif = TRUE
            ORDER BY code
            """,
            nativeQuery = true)
    List<OptionRow> findOptionsActives();

}
