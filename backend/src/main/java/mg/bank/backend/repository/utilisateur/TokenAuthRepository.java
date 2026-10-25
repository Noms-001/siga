package mg.bank.backend.repository.utilisateur;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.utilisateur.TokenAuth;

public interface TokenAuthRepository extends JpaRepository<TokenAuth, Integer> {
    Optional<TokenAuth> findByToken(String token);
    
}
