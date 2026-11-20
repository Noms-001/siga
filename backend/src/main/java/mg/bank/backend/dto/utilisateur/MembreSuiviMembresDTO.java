package mg.bank.backend.dto.utilisateur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MembreSuiviMembresDTO(
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean filtreServiceVisible,
        List<MembreSuiviGlobalDTO.ServiceOptionDTO> servicesDisponibles,
        long membresSuivis,
        long membresAvecActivites,
        long membresAvecPlansActions,
        long membresAvecRetards,
        List<MembreDTO> membres) {

    public record MembreDTO(
            Integer idUtilisateur,
            String nom,
            String prenom,
            String nomComplet,
            Integer idService,
            String service,
            long totalActivites,
            long totalPta,
            long totalNonPta,
            BigDecimal avancementPta, // NEW
            BigDecimal avancementNonPta, // NEW
            BigDecimal avancementActivites,
            long activitesPtaEnRetard, // NEW
            long activitesNonPtaEnRetard, // NEW
            long totalPlansActions,
            BigDecimal avancementPlansActions,
            long plansActionsEnRetard, // NEW
            BigDecimal avancementGlobal,
            long activitesEnRetard, // NEW
            long elementsEnRetard) {
    }
}
