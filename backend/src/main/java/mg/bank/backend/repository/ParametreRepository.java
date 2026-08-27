package mg.bank.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.Parametre;

public interface ParametreRepository extends JpaRepository<Parametre, Integer> {
    Optional<Parametre> findByCodeAndActifTrue(String code);
}
