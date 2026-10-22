package mg.bank.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.ValeurIndicateur;

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

    // ── valeur courante (période terminée) ────────────────────────
    /**
     * Derniere valeur dont la periode est terminee (periode_fin <= reference).
     * Si aucune, retourne la derniere valeur saisie, quelle que soit sa periode.
     * Cette "reserve" evite qu'un indicateur sans historique termine n'affiche
     * rien alors qu'une saisie existe.
     */
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
    boolean existsByIdIndicateurAndPeriodeDebutAndPeriodeFin(
            Integer idIndicateur, LocalDateTime periodeDebut, LocalDateTime periodeFin);

    Optional<ValeurIndicateur> findFirstByIdIndicateurAndPeriodeDebutAndPeriodeFin(
            Integer idIndicateur, LocalDateTime periodeDebut, LocalDateTime periodeFin);
}