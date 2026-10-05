package mg.bank.backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.Parametre;

public interface ParametreRepository extends JpaRepository<Parametre, Integer> {
    Optional<Parametre> findByCodeAndActifTrue(String code);

    Optional<Parametre> findByCode(String code);

    boolean existsByCode(String code);

    @Query("select distinct p.categorie from Parametre p where p.categorie is not null order by p.categorie")
    List<String> findDistinctCategories();

    @Query("""
            select p from Parametre p
            where (:actif is null or p.actif = :actif)
              and (:categorie is null or p.categorie = :categorie)
            order by p.categorie, p.code
            """)
    List<Parametre> rechercher(
            @Param("actif") Boolean actif,
            @Param("categorie") String categorie);
}
