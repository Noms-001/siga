package mg.bank.backend.repository.referentiel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.referentiel.Statut;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.projection.common.OptionRow;

import java.util.Optional;

public interface StatutRepository extends JpaRepository<Statut, Integer> {

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

    Optional<Statut> findByCode(String code);

    List<Statut> findByActifTrue();

}
