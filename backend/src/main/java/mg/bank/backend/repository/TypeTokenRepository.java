package mg.bank.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.TypeToken;

public interface TypeTokenRepository extends JpaRepository<TypeToken, Integer> {
    Optional<TypeToken> findByCode(String code);
}
