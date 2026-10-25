package mg.bank.backend.repository.referentiel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.referentiel.Priorite;
import mg.bank.backend.repository.projection.common.OptionRow;

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
            """, nativeQuery = true)
    List<OptionRow> findOptionsActives();

    List<Priorite> findByActifTrue();

    boolean existsByCodeIgnoreCase(String code);

    Optional<Priorite> findByCodeIgnoreCase(String code);

}
