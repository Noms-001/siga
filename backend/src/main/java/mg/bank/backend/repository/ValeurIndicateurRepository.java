package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.ValeurIndicateur;

public interface ValeurIndicateurRepository
        extends JpaRepository<ValeurIndicateur, Integer> {

    @Query("""
            SELECT v FROM ValeurIndicateur v
            JOIN FETCH v.utilisateur
            WHERE v.indicateur.idIndicateur = :idIndicateur
            ORDER BY v.periodeFin DESC, v.idValeurIndicateur DESC
            """)
    List<ValeurIndicateur> findHistoriqueByIndicateur(
            @Param("idIndicateur") Integer idIndicateur);
}