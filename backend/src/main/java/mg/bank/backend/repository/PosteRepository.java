package mg.bank.backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.Poste;

public interface PosteRepository extends JpaRepository<Poste, Integer> {
    Optional<Poste> findFirstByUtilisateurs_IdUtilisateurAndIsMetier(
            Integer idUtilisateur,
            Boolean isMetier
    );

    List<Poste> findByActifTrue();

    List<Poste> findByActifFalse();

    boolean existsByLibelle(String libelle);

    boolean existsByLibelleAndIdPosteNot(String libelle, Integer idPoste);
}
