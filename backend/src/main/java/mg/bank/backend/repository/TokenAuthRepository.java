package mg.bank.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.TokenAuth;

public interface TokenAuthRepository extends JpaRepository<TokenAuth, Integer> {
    Optional<TokenAuth> findByToken(String token);
    
}
