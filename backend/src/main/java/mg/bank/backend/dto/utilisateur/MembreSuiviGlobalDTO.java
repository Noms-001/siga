package mg.bank.backend.dto.utilisateur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MembreSuiviGlobalDTO(
        LocalDate dateDebut,
        LocalDate dateFin,
        String categorieSelectionnee,
        boolean comparaisonServicesVisible,
        long nombreMembres,
        long totalActivitesPta,
        long totalActivitesNonPta,
        long totalPlansActions,
        BigDecimal avancementGlobal,
        long elementsTermines,
        long elementsEnRetard,
        List<CategorieDTO> categories,
        List<StatutDTO> statutsActivites,
        List<EvolutionMensuelleDTO> evolutionMensuelle,
        List<ServiceDTO> services,
        List<ServiceOptionDTO> servicesDisponibles,
        long plansMultiServicesNonRepartis) {

    public record CategorieDTO(
            String categorie,
            String libelle,
            long total,
            long termines,
            long enCours,
            long enRetard,
            BigDecimal avancement) { }

    public record StatutDTO(String statut, String libelle, long nombre) { }

    public record EvolutionMensuelleDTO(
            String mois,
            BigDecimal pta,
            BigDecimal nonPta,
            BigDecimal plansActions) { }

    public record ServiceDTO(
            Integer idService,
            String service,
            BigDecimal avancementPta,
            BigDecimal avancementNonPta,
            BigDecimal avancementPlansActions) { }

    public record ServiceOptionDTO(Integer id, String nom) { }

    public record ElementDTO(
            Integer id,
            String categorie,
            String code,
            String designation,
            String service,
            String statut,
            LocalDate dateEcheance,
            BigDecimal avancement) { }
}
