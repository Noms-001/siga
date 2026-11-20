package mg.bank.backend.service.utilisateur;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.utilisateur.MembreSuiviGlobalDTO;
import mg.bank.backend.dto.utilisateur.MembreSuiviMembresDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.repository.projection.common.OptionRow;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.repository.utilisateur.MembreSuiviRepository;

@Service
@RequiredArgsConstructor
public class MembreSuiviService {

    private static final Set<String> CATEGORIES = Set.of("PTA", "NON_PTA", "PLANS_ACTIONS");
    private static final List<String> ORDRE_CATEGORIES = List.of("PTA", "NON_PTA", "PLANS_ACTIONS");
    private static final List<String> ORDRE_STATUTS = List.of(
            "NON_COMMENCEE", "EN_COURS", "EN_RETARD", "TERMINEE", "SUSPENDUE", "REPORTEE", "ANNULEE");

    private final MembreSuiviRepository repository;
    private final CurrentUserService currentUserService;
    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public MembreSuiviMembresDTO membres(
            LocalDate dateDebut,
            LocalDate dateFin,
            Integer annee,
            Integer trimestre,
            Integer idServiceDemande,
            String recherche) {
        Periode periode = resoudrePeriode(dateDebut, dateFin, annee, trimestre);
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        verifierPerimetre(perimetre);
        Integer idService = resoudreService(perimetre, idServiceDemande);
        MapSqlParameterSource params = parametres(periode, perimetre, idService, null);
        List<MembreSuiviMembresDTO.MembreDTO> membres = repository.findMembres(params, recherche).stream()
                .map(this::versMembre)
                .toList();
        List<MembreSuiviGlobalDTO.ServiceOptionDTO> servicesDisponibles = perimetre.estNiveauDepartement()
                ? serviceRepository.findOptionsActivesParDepartement(perimetre.getIdDepartement()).stream()
                        .map(this::serviceOption)
                        .toList()
                : List.of();

        return new MembreSuiviMembresDTO(
                periode.debut(),
                periode.fin(),
                perimetre.estNiveauDepartement(),
                servicesDisponibles,
                membres.size(),
                membres.stream().filter(membre -> membre.totalActivites() > 0).count(),
                membres.stream().filter(membre -> membre.totalPlansActions() > 0).count(),
                membres.stream().filter(membre -> membre.elementsEnRetard() > 0).count(),
                membres);
    }

    @Transactional(readOnly = true)
    public MembreSuiviGlobalDTO global(
            LocalDate dateDebut,
            LocalDate dateFin,
            Integer annee,
            Integer trimestre,
            Integer idServiceDemande,
            String categorieDemandee) {
        Periode periode = resoudrePeriode(dateDebut, dateFin, annee, trimestre);
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        verifierPerimetre(perimetre);
        Integer idService = resoudreService(perimetre, idServiceDemande);
        String categorie = normaliserCategorie(categorieDemandee);
        MapSqlParameterSource params = parametres(periode, perimetre, idService, categorie);

        List<MembreSuiviGlobalDTO.CategorieDTO> categories = categories(repository.findCategories(params), categorie);
        long totalPta = total(categories, "PTA");
        long totalNonPta = total(categories, "NON_PTA");
        long totalPlans = total(categories, "PLANS_ACTIONS");
        long totalElements = categories.stream().mapToLong(MembreSuiviGlobalDTO.CategorieDTO::total).sum();
        BigDecimal sommeAvancements = categories.stream()
                .map(row -> row.avancement().multiply(BigDecimal.valueOf(row.total())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avancementGlobal = totalElements == 0 ? BigDecimal.ZERO
                : sommeAvancements.divide(BigDecimal.valueOf(totalElements), 2, RoundingMode.HALF_UP);
        long termines = categories.stream().mapToLong(MembreSuiviGlobalDTO.CategorieDTO::termines).sum();
        long enRetard = categories.stream().mapToLong(MembreSuiviGlobalDTO.CategorieDTO::enRetard).sum();

        List<MembreSuiviGlobalDTO.StatutDTO> statuts = statuts(repository.findStatutsActivites(params), categorie);
        List<MembreSuiviGlobalDTO.EvolutionMensuelleDTO> evolution = evolution(
                repository.findEvolution(params), periode, categorie);
        boolean comparaisonServicesVisible = perimetre.estNiveauDepartement() && idService == null;
        List<MembreSuiviGlobalDTO.ServiceDTO> services = comparaisonServicesVisible
                ? services(repository.findServices(params), categorie)
                : List.of();
        List<MembreSuiviGlobalDTO.ServiceOptionDTO> servicesDisponibles = perimetre.estNiveauDepartement()
                ? serviceRepository.findOptionsActivesParDepartement(perimetre.getIdDepartement()).stream()
                        .map(this::serviceOption)
                        .toList()
                : List.of();
        long multiServices = perimetre.estNiveauDepartement() && idService == null
                && (categorie == null || "PLANS_ACTIONS".equals(categorie))
                        ? repository.countPlansMultiServices(params)
                        : 0;

        return new MembreSuiviGlobalDTO(
                periode.debut(), periode.fin(), categorie, comparaisonServicesVisible,
                repository.countMembres(params), totalPta, totalNonPta, totalPlans,
                avancementGlobal, termines, enRetard, categories, statuts, evolution,
                services, servicesDisponibles, multiServices);
    }

    @Transactional(readOnly = true)
    public List<MembreSuiviGlobalDTO.ElementDTO> elements(
            LocalDate dateDebut,
            LocalDate dateFin,
            Integer annee,
            Integer trimestre,
            Integer idServiceDemande,
            String categorieDemandee) {
        Periode periode = resoudrePeriode(dateDebut, dateFin, annee, trimestre);
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        verifierPerimetre(perimetre);
        Integer idService = resoudreService(perimetre, idServiceDemande);
        String categorie = normaliserCategorie(categorieDemandee);
        if (categorie == null) {
            throw new ApiException("La catégorie est obligatoire pour consulter les éléments", HttpStatus.BAD_REQUEST);
        }
        return repository.findElements(parametres(periode, perimetre, idService, categorie)).stream()
                .map(row -> new MembreSuiviGlobalDTO.ElementDTO(
                        integer(row.get("id")), string(row.get("categorie")), string(row.get("code")),
                        string(row.get("designation")), string(row.get("service")), string(row.get("statut")),
                        localDate(row.get("date_echeance")), decimal(row.get("avancement"))))
                .toList();
    }

    private Periode resoudrePeriode(LocalDate debut, LocalDate fin, Integer annee, Integer trimestre) {
        if ((debut == null) != (fin == null)) {
            throw new ApiException("Les deux dates de la période personnalisée sont obligatoires",
                    HttpStatus.BAD_REQUEST);
        }
        if (debut != null) {
            if (annee != null || trimestre != null || fin == null || fin.isBefore(debut)) {
                throw new ApiException("Période personnalisée invalide", HttpStatus.BAD_REQUEST);
            }
            return new Periode(debut, fin);
        }
        int selectedYear = annee == null ? LocalDate.now().getYear() : annee;
        if (selectedYear < 1900 || selectedYear > 2100 || (trimestre != null && (trimestre < 1 || trimestre > 4))) {
            throw new ApiException("Année ou trimestre invalide", HttpStatus.BAD_REQUEST);
        }
        if (trimestre == null) {
            return new Periode(LocalDate.of(selectedYear, 1, 1), LocalDate.of(selectedYear, 12, 31));
        }
        int firstMonth = (trimestre - 1) * 3 + 1;
        YearMonth first = YearMonth.of(selectedYear, firstMonth);
        return new Periode(first.atDay(1), first.plusMonths(2).atEndOfMonth());
    }

    private void verifierPerimetre(PerimetreUtilisateur perimetre) {
        if (perimetre.estSansPerimetre() || (!perimetre.estNiveauDepartement() && !perimetre.estRattacheService())) {
            throw new ApiException("Un rattachement à un service ou département est requis", HttpStatus.FORBIDDEN);
        }
    }

    private Integer resoudreService(PerimetreUtilisateur perimetre, Integer demande) {
        if (perimetre.estRattacheService()) {
            if (demande != null && !demande.equals(perimetre.getIdService())) {
                throw new ApiException("Accès interdit à ce service", HttpStatus.FORBIDDEN);
            }
            return perimetre.getIdService();
        }
        if (demande != null) {
            mg.bank.backend.model.referentiel.Service service = serviceRepository.findById(demande)
                    .orElseThrow(() -> new ApiException("Service introuvable", HttpStatus.NOT_FOUND));
            if (service.getDepartement() == null
                    || !perimetre.getIdDepartement().equals(service.getDepartement().getIdDepartement())) {
                throw new ApiException("Accès interdit à ce service", HttpStatus.FORBIDDEN);
            }
        }
        return demande;
    }

    private String normaliserCategorie(String value) {
        if (value == null || value.isBlank() || "TOUTES".equalsIgnoreCase(value)
                || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("PLAN_ACTION".equals(normalized) || "PLANS_ACTION".equals(normalized)) {
            normalized = "PLANS_ACTIONS";
        }
        if (!CATEGORIES.contains(normalized)) {
            throw new ApiException("Catégorie invalide", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private MapSqlParameterSource parametres(
            Periode periode, PerimetreUtilisateur perimetre, Integer idService, String categorie) {
        return new MapSqlParameterSource()
                .addValue("dateDebut", periode.debut())
                .addValue("dateFin", periode.fin())
                .addValue("idDepartement", perimetre.getIdDepartement())
                .addValue("idService", idService)
                .addValue("categorie", categorie);
    }

    private List<MembreSuiviGlobalDTO.CategorieDTO> categories(List<Map<String, Object>> rows,
            String categorieSelectionnee) {
        Map<String, MembreSuiviGlobalDTO.CategorieDTO> values = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String code = string(row.get("categorie"));
            values.put(code, new MembreSuiviGlobalDTO.CategorieDTO(
                    code, libelleCategorie(code), number(row.get("total")), number(row.get("termines")),
                    number(row.get("en_cours")), number(row.get("en_retard")), decimal(row.get("avancement"))));
        }
        return ORDRE_CATEGORIES.stream().map(code -> {
            MembreSuiviGlobalDTO.CategorieDTO row = values.getOrDefault(code,
                    new MembreSuiviGlobalDTO.CategorieDTO(code, libelleCategorie(code), 0, 0, 0, 0, BigDecimal.ZERO));
            return categorieSelectionnee != null && !categorieSelectionnee.equals(code)
                    ? new MembreSuiviGlobalDTO.CategorieDTO(code, libelleCategorie(code), 0, 0, 0, 0, BigDecimal.ZERO)
                    : row;
        }).toList();
    }

    private List<MembreSuiviGlobalDTO.StatutDTO> statuts(List<Map<String, Object>> rows, String categorie) {
        Map<String, Long> counts = new HashMap<>();
        if (!"PLANS_ACTIONS".equals(categorie)) {
            rows.forEach(row -> counts.put(string(row.get("statut_code")), number(row.get("nombre"))));
        }
        return ORDRE_STATUTS.stream()
                .map(code -> new MembreSuiviGlobalDTO.StatutDTO(code, libelleStatut(code),
                        counts.getOrDefault(code, 0L)))
                .toList();
    }

    private List<MembreSuiviGlobalDTO.EvolutionMensuelleDTO> evolution(
            List<Map<String, Object>> rows, Periode periode, String categorie) {
        Map<String, Map<String, BigDecimal>> byMonth = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String month = monthString(row.get("debut_mois"));
            byMonth.computeIfAbsent(month, ignored -> new HashMap<>())
                    .put(string(row.get("categorie")), nullableDecimal(row.get("avancement")));
        }
        LocalDate until = periode.fin().isAfter(LocalDate.now()) ? LocalDate.now() : periode.fin();
        YearMonth month = YearMonth.from(periode.debut());
        YearMonth last = YearMonth.from(until);
        List<MembreSuiviGlobalDTO.EvolutionMensuelleDTO> result = new ArrayList<>();
        while (!month.isAfter(last)) {
            Map<String, BigDecimal> values = byMonth.getOrDefault(month.toString(), Map.of());
            result.add(new MembreSuiviGlobalDTO.EvolutionMensuelleDTO(
                    month.toString(),
                    isSelected(categorie, "PTA") ? values.get("PTA") : null,
                    isSelected(categorie, "NON_PTA") ? values.get("NON_PTA") : null,
                    isSelected(categorie, "PLANS_ACTIONS") ? values.get("PLANS_ACTIONS") : null));
            month = month.plusMonths(1);
        }
        return result;
    }

    private List<MembreSuiviGlobalDTO.ServiceDTO> services(List<Map<String, Object>> rows, String categorie) {
        return rows.stream().map(row -> new MembreSuiviGlobalDTO.ServiceDTO(
                integer(row.get("id_service")), string(row.get("nom_service")),
                isSelected(categorie, "PTA") ? decimal(row.get("pta")) : BigDecimal.ZERO,
                isSelected(categorie, "NON_PTA") ? decimal(row.get("non_pta")) : BigDecimal.ZERO,
                isSelected(categorie, "PLANS_ACTIONS") ? decimal(row.get("plans_actions")) : BigDecimal.ZERO))
                .toList();
    }

    private MembreSuiviGlobalDTO.ServiceOptionDTO serviceOption(OptionRow row) {
        return new MembreSuiviGlobalDTO.ServiceOptionDTO(row.getId(), row.getLibelle());
    }

    private MembreSuiviMembresDTO.MembreDTO versMembre(Map<String, Object> row) {
        String nom = string(row.get("nom"));
        String prenom = string(row.get("prenom"));
        return new MembreSuiviMembresDTO.MembreDTO(
                integer(row.get("id_utilisateur")),
                nom,
                prenom,
                ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim(),
                integer(row.get("id_service")),
                string(row.get("service")),
                number(row.get("total_activites")),
                number(row.get("total_pta")),
                number(row.get("total_non_pta")),
                nullableDecimal(row.get("avancement_pta")), // NEW
                nullableDecimal(row.get("avancement_non_pta")), // NEW
                nullableDecimal(row.get("avancement_activites")),
                number(row.get("activites_pta_en_retard")),       // NEW
                number(row.get("activites_non_pta_en_retard")),  
                number(row.get("total_plans")),
                nullableDecimal(row.get("avancement_plans")),
                number(row.get("plans_actions_en_retard")), 
                nullableDecimal(row.get("avancement_global")),
                number(row.get("activites_en_retard")), // NEW
                number(row.get("elements_en_retard")));
    }

    private boolean isSelected(String selection, String category) {
        return selection == null || selection.equals(category);
    }

    private long total(List<MembreSuiviGlobalDTO.CategorieDTO> categories, String category) {
        return categories.stream().filter(row -> row.categorie().equals(category))
                .mapToLong(MembreSuiviGlobalDTO.CategorieDTO::total).findFirst().orElse(0);
    }

    private String libelleCategorie(String code) {
        return switch (code) {
            case "PTA" -> "Activités PTA";
            case "NON_PTA" -> "Activités NON_PTA";
            default -> "Plans d’actions";
        };
    }

    private String libelleStatut(String code) {
        return switch (code) {
            case "NON_COMMENCEE" -> "Non commencée";
            case "EN_COURS" -> "En cours";
            case "EN_RETARD" -> "En retard";
            case "TERMINEE" -> "Terminée";
            case "SUSPENDUE" -> "Suspendue";
            case "REPORTEE" -> "Reportée";
            default -> "Annulée";
        };
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullableDecimal(Object value) {
        return value == null ? null : decimal(value);
    }

    private LocalDate localDate(Object value) {
        if (value instanceof Date date)
            return date.toLocalDate();
        if (value instanceof LocalDate date)
            return date;
        return value == null ? null : LocalDate.parse(value.toString());
    }

    private String monthString(Object value) {
        if (value instanceof Date date)
            return date.toLocalDate().toString().substring(0, 7);
        if (value instanceof LocalDate date)
            return date.toString().substring(0, 7);
        return value == null ? "" : value.toString().substring(0, 7);
    }

    private record Periode(LocalDate debut, LocalDate fin) {
    }
}
