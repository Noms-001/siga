package mg.bank.backend.repository.activite;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.activite.SousActivite;

public interface SousActiviteRepository
        extends JpaRepository<SousActivite, Integer> {

    List<SousActivite> findByActiviteIdActiviteOrderByDateDebutPrevueAscCodeAsc(
            Integer idActivite);

    boolean existsByCodeIgnoreCase(String code);

    @Query(value = """
            SELECT
              (SELECT COUNT(*) FROM livrable_sous_activite
                WHERE id_sous_activite = :id)
            + (SELECT COUNT(*) FROM affectation_sous_activite
                WHERE id_sous_activite = :id)
            + (SELECT COUNT(*) FROM avancement_sous_activite
                WHERE id_sous_activite = :id)
            """,
            nativeQuery = true)
    long compterDependances(@Param("id") Integer idSousActivite);

}
