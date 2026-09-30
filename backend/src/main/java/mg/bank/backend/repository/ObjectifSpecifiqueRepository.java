package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.ObjectifSpecifique;
import mg.bank.backend.repository.projection.OptionRow;

public interface ObjectifSpecifiqueRepository
        extends JpaRepository<ObjectifSpecifique, Integer> {

    /**
     * Autocomplete objectif : code ou designation.
     */
    @Query(value = """
            SELECT id_objectif_specifique AS id,
                   code                   AS code,
                   designation            AS libelle
            FROM objectif_specifique
            WHERE (:recherche IS NULL
                   OR code ILIKE :motif
                   OR designation ILIKE :motif)
            ORDER BY code
            LIMIT :limite
            """,
            nativeQuery = true)
    List<OptionRow> autocomplete(
            String recherche,
            String motif,
            int limite);

}
