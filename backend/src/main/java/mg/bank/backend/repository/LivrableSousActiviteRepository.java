package mg.bank.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mg.bank.backend.model.LivrableSousActivite;

/**
 * Repository des livrables rattaches a une sous-activite.
 *
 * <p>Seul {@code save} est utilise par l'import : les livrables d'une
 * sous-activite existante sont lus par {@code ActiviteRepository.findLivrables}
 * (requete native), qui ne passe pas par ce repository.</p>
 */
@Repository
public interface LivrableSousActiviteRepository
        extends JpaRepository<LivrableSousActivite, Integer> {
}