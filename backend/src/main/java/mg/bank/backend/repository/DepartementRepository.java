package mg.bank.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.Departement;

public interface DepartementRepository extends JpaRepository<Departement, Integer> {
    Optional<Departement> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdDepartementNot(String code, Integer idDepartement);
}
