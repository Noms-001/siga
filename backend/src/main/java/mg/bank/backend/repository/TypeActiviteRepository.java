package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.TypeActivite;
import mg.bank.backend.repository.projection.OptionRow;

public interface TypeActiviteRepository extends JpaRepository<TypeActivite, Integer> {

    /**
     * Types d'activite actifs, pour le filtre.
     */
    @Query(value = """
            SELECT id_type_activite AS id,
                   NULL             AS code,
                   designation      AS libelle
            FROM type_activite
            WHERE actif = TRUE
            ORDER BY designation
            """,
            nativeQuery = true)
    List<OptionRow> findOptionsActives();

}
