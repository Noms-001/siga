package mg.bank.backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByEmail(String email);
    Boolean existsByEmail(String email);
    List<Utilisateur> findByActifTrue();

    List<Utilisateur> findByActifFalse();

    @Query("""
            select u from Utilisateur u
            left join fetch u.departement
            left join fetch u.service
            where u.idUtilisateur = :id
            """)
    Optional<Utilisateur> findByIdWithRelations(@Param("id") Integer id);

    @Query("""
            select distinct u from Utilisateur u
            left join fetch u.departement
            left join fetch u.service
            where (:actif is null or u.actif = :actif)
            and (:idDepartement is null or u.departement.idDepartement = :idDepartement)
            and (:idService is null or u.service.idService = :idService)
            order by u.nom, u.prenom
            """)
    List<Utilisateur> rechercher(
            @Param("actif") Boolean actif,
            @Param("idDepartement") Integer idDepartement,
            @Param("idService") Integer idService);
}
