package mg.bank.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByEmail(String email);
}
