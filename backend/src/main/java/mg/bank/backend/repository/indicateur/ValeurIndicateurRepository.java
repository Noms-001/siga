package mg.bank.backend.repository.indicateur;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.indicateur.ValeurIndicateur;

public interface ValeurIndicateurRepository
                extends JpaRepository<ValeurIndicateur, Integer> {

        // ── méthode existante ─────────────────────────────────────────
        @Query("""
                        SELECT v FROM ValeurIndicateur v
                        JOIN FETCH v.utilisateur
                        WHERE v.indicateur.idIndicateur = :idIndicateur
                        ORDER BY v.periodeFin DESC, v.idValeurIndicateur DESC
                        """)
        List<ValeurIndicateur> findHistoriqueByIndicateur(
                        @Param("idIndicateur") Integer idIndicateur);

        @Query("""
                        SELECT v FROM ValeurIndicateur v
                        JOIN FETCH v.utilisateur
                        WHERE v.indicateur.idIndicateur = :idIndicateur
                          AND v.periodeFin <= :reference
                        ORDER BY v.periodeFin DESC, v.idValeurIndicateur DESC
                        """)
        List<ValeurIndicateur> findValeursTerminees(
                        @Param("idIndicateur") Integer idIndicateur,
                        @Param("reference") LocalDateTime reference);

        @Query("""
                        SELECT v FROM ValeurIndicateur v
                        JOIN FETCH v.utilisateur
                        WHERE v.indicateur.idIndicateur = :idIndicateur
                        ORDER BY v.periodeFin DESC, v.idValeurIndicateur DESC
                        """)
        List<ValeurIndicateur> findToutesValeurs(
                        @Param("idIndicateur") Integer idIndicateur);

        // ── détection de doublon de période ────────────────────────────
        boolean existsByIndicateur_IdIndicateurAndPeriodeDebutAndPeriodeFin(
                        Integer idIndicateur, LocalDateTime periodeDebut, LocalDateTime periodeFin);

        @Query("""
                        SELECT v FROM ValeurIndicateur v
                        JOIN FETCH v.utilisateur
                        WHERE v.indicateur.idIndicateur IN :ids
                        ORDER BY v.indicateur.idIndicateur, v.periodeFin DESC, v.idValeurIndicateur DESC
                        """)
        List<ValeurIndicateur> findToutesValeursPourIndicateurs(
                        @Param("ids") List<Integer> ids);
}