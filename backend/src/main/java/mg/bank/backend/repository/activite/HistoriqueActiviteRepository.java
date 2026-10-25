package mg.bank.backend.repository.activite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mg.bank.backend.model.activite.HistoriqueActivite;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoriqueActiviteRepository
        extends JpaRepository<HistoriqueActivite, Integer> {

    List<HistoriqueActivite>
    findByActivite_IdActiviteOrderByDateChangementDesc(
            Integer idActivite
    );

    Optional<HistoriqueActivite>
    findFirstByActivite_IdActiviteOrderByDateChangementDesc(
            Integer idActivite
    );
}