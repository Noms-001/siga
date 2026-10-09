package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.ValeurIndicateur;

public interface ValeurIndicateurRepository
        extends JpaRepository<ValeurIndicateur, Integer> {

    /**
     * Historique des valeurs d'un indicateur, du plus récent au plus
     * ancien.
     *
     * JOIN FETCH sur utilisateur : sans lui, chaque ligne déclencherait
     * une requête supplémentaire (N+1) au moment du mapping. Le second
     * critère de tri (id) tranche deux relevés qui partageraient la même
     * période de fin — l'ordre reste déterministe d'un appel à l'autre.
     */
    @Query("""
            SELECT v FROM ValeurIndicateur v
            JOIN FETCH v.utilisateur
            WHERE v.indicateur.idIndicateur = :idIndicateur
            ORDER BY v.periodeFin DESC, v.idValeurIndicateur DESC
            """)
    List<ValeurIndicateur> findHistoriqueByIndicateur(
            @Param("idIndicateur") Integer idIndicateur);
}