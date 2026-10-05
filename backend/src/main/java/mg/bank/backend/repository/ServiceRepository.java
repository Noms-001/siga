package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.Service;
import mg.bank.backend.repository.projection.OptionRow;

public interface ServiceRepository extends JpaRepository<Service, Integer> {

    /**
     * Services actifs d'un departement.
     *
     * Utilise pour alimenter le filtre Service d'un utilisateur de niveau
     * departement. Un utilisateur rattache a un service ne le voit pas :
     * le perimetre est bloque cote service.
     */
    @Query(value = """
            SELECT id_service AS id,
                   NULL       AS code,
                   nom        AS libelle
            FROM service
            WHERE id_departement = :idDepartement
              AND actif = TRUE
            ORDER BY nom
            """,
            nativeQuery = true)
    List<OptionRow> findOptionsActivesParDepartement(Integer idDepartement);

    List<Service> findByActifTrue();

    List<Service> findByDepartementIdDepartement(Integer idDepartement);

    List<Service> findByDepartementIdDepartementAndActifTrue(Integer idDepartement);

    boolean existsByNomAndDepartementIdDepartement(String nom, Integer idDepartement);

    boolean existsByNomAndDepartementIdDepartementAndIdServiceNot(
            String nom, Integer idDepartement, Integer idService);

}
