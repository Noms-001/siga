package mg.bank.backend.repository.utilisateur;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.bank.backend.model.utilisateur.Poste;

public interface PosteRepository extends JpaRepository<Poste, Integer> {

    List<Poste> findByActifTrue();

    List<Poste> findByActifFalse();

    boolean existsByLibelle(String libelle);

    boolean existsByLibelleAndIdPosteNot(String libelle, Integer idPoste);
}
