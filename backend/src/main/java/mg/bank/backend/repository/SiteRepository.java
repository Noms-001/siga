package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.bank.backend.model.Site;
import mg.bank.backend.model.Statut;
import mg.bank.backend.repository.projection.OptionRow;

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


}
