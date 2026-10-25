package mg.bank.backend.repository.referentiel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.referentiel.Site;
import mg.bank.backend.repository.projection.common.OptionRow;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    /**
     * Sites actifs, pour le filtre.
     */
    @Query(value = """
            SELECT id_site AS id,
                   NULL     AS code,
                   nom      AS libelle
            FROM site
            WHERE actif = TRUE
            ORDER BY nom
            """,
            nativeQuery = true)
    List<OptionRow> findOptionsActives();

    List<Site> findByActifTrue();

    boolean existsByNom(String nom);

    boolean existsByNomAndIdSiteNot(String nom, Integer idSite);

    long countByNomIgnoreCase(String nom);

    Optional<Site> findByNomIgnoreCase(String nom);

}
