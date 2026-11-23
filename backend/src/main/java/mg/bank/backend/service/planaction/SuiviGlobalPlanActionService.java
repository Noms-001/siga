package mg.bank.backend.service.planaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.planaction.SuiviGlobalPlanActionDTO;
import mg.bank.backend.repository.planaction.PlanActionRepository;
import mg.bank.backend.repository.projection.planaction.PlanActionSuiviRow;
import mg.bank.backend.repository.projection.planaction.PlanActionTypeOrigineRow;
import mg.bank.backend.service.common.PeriodeSupport.Periode;
import mg.bank.backend.service.utilisateur.CurrentUserService;


@Service
@RequiredArgsConstructor
public class SuiviGlobalPlanActionService {

    private static final String[] STATUTS_ACTIFS =
            { "NON_COMMENCEE", "EN_COURS", "TERMINEE" };

    private static final int SEUIL_ECHEANCE_JOURS = 30;
    private static final int LIMITE_RESPONSABLES = 10;
    private static final int LIMITE_RETARD = 20;
    private static final int LIMITE_ECHEANCES = 20;

    private final PlanActionRepository planActionRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public SuiviGlobalPlanActionDTO suiviGlobal(Periode periode) {

        // 1. Lire tous les plans actifs (statuts non BROUILLON/ANNULEE)
        String statutsCsv = String.join(",", STATUTS_ACTIFS);
        var perimetre = currentUserService.getPerimetre();
        List<PlanActionSuiviRow> plans = planActionRepository.findPlansPourSuivi(
                statutsCsv, periode.debut(), periode.fin(), periode.reference(),
                perimetre.getFiltreService(), perimetre.getFiltreDepartement(),
                perimetre.getIdUtilisateurPortee());

        if (plans.isEmpty()) {
            return SuiviGlobalPlanActionDTO.builder()
                    .resume(SuiviGlobalPlanActionDTO.ResumeGlobalDTO.builder()
                            .total(0).nonCommences(0).enCours(0).enRetard(0).termines(0)
                            .avancementMoyen(BigDecimal.ZERO).build())
                    .parTypeOrigine(List.of())
                    .parResponsable(List.of())
                    .enRetard(List.of())
                    .echeancesProchaines(List.of())
                    .build();
        }

        LocalDate aujourdHui = periode.reference();

        // 2. Repartition des statuts
        Set<Integer> idsPlans = plans.stream()
                .map(PlanActionSuiviRow::getId)
                .collect(Collectors.toSet());

        List<PlanActionSuiviRow> enRetard = new ArrayList<>();
        int nonCommences = 0;
        int enCours = 0;
        int termines = 0;
        BigDecimal sommeAvancement = BigDecimal.ZERO;

        for (PlanActionSuiviRow p : plans) {
            BigDecimal av = p.getAvancement() == null ? BigDecimal.ZERO : p.getAvancement();
            sommeAvancement = sommeAvancement.add(av);

            if ("TERMINEE".equals(p.getStatutCode())) {
                termines++;
            } else if ("NON_COMMENCEE".equals(p.getStatutCode())) {
                nonCommences++;
            } else {
                enCours++;
            }

            // En retard : date fin prevue < aujourd'hui et pas terminé
            if (p.getDateFinPrevue() != null
                    && p.getDateFinPrevue().isBefore(aujourdHui)
                    && !"TERMINEE".equals(p.getStatutCode())) {
                enRetard.add(p);
            }
        }

        // 3. Répartition par origine principale (ou, à défaut, la première
        // origine liée selon son identifiant). Chaque plan ne compte qu'une fois.
        List<PlanActionSuiviRow> finalPlans = plans;
        Map<Integer, String> typeParPlan = new HashMap<>();
        if (!idsPlans.isEmpty()) {
            List<PlanActionTypeOrigineRow> typesRows = planActionRepository
                    .findTypesOriginePourPlans(new ArrayList<>(idsPlans));
            for (PlanActionTypeOrigineRow r : typesRows) {
                typeParPlan.put(r.getIdPlanAction(), r.getTypeOrigine());
            }
        }

        Map<String, List<BigDecimal>> avancementsParType = new HashMap<>();
        Map<String, Integer> compteurParType = new HashMap<>();

        for (PlanActionSuiviRow p : finalPlans) {
                        String type = typeParPlan.get(p.getId());
                        if (type != null) {
                compteurParType.merge(type, 1, Integer::sum);
                avancementsParType
                        .computeIfAbsent(type, k -> new ArrayList<>())
                        .add(p.getAvancement() == null ? BigDecimal.ZERO : p.getAvancement());
            }
        }

        List<SuiviGlobalPlanActionDTO.RepartitionOrigineDTO> parTypeOrigine =
                compteurParType.entrySet().stream()
                        .map(e -> SuiviGlobalPlanActionDTO.RepartitionOrigineDTO.builder()
                                .typeOrigine(e.getKey())
                                .nombre(e.getValue())
                                .avancementMoyen(moyenne(avancementsParType.get(e.getKey())))
                                .build())
                        .sorted(Comparator.comparing(
                                SuiviGlobalPlanActionDTO.RepartitionOrigineDTO::getNombre).reversed())
                        .toList();

        // 4. Répartition par responsable
        Map<Integer, List<PlanActionSuiviRow>> parResponsable = new HashMap<>();
        for (PlanActionSuiviRow p : finalPlans) {
            if (p.getResponsableId() != null) {
                parResponsable.computeIfAbsent(p.getResponsableId(), k -> new ArrayList<>()).add(p);
            }
        }

        List<SuiviGlobalPlanActionDTO.RepartitionResponsableDTO> repartitionResponsables =
                parResponsable.entrySet().stream()
                        .map(e -> {
                            List<PlanActionSuiviRow> plansResp = e.getValue();
                            long enRetardResp = plansResp.stream()
                                    .filter(p -> p.getDateFinPrevue() != null
                                            && p.getDateFinPrevue().isBefore(aujourdHui)
                                            && !"TERMINEE".equals(p.getStatutCode()))
                                    .count();
                            List<BigDecimal> avs = plansResp.stream()
                                    .map(p -> p.getAvancement() == null
                                            ? BigDecimal.ZERO : p.getAvancement())
                                    .toList();
                            PlanActionSuiviRow first = plansResp.get(0);
                            return SuiviGlobalPlanActionDTO.RepartitionResponsableDTO.builder()
                                    .idUtilisateur(e.getKey())
                                    .libelle(first.getResponsablePrenom() + " " + first.getResponsableNom())
                                    .total(plansResp.size())
                                    .enRetard((int) enRetardResp)
                                    .avancementMoyen(moyenne(avs))
                                    .build();
                        })
                        .sorted(Comparator.comparing(
                                SuiviGlobalPlanActionDTO.RepartitionResponsableDTO::getTotal).reversed())
                        .limit(LIMITE_RESPONSABLES)
                        .toList();

        // 5. Plans en retard (top N)
        List<SuiviGlobalPlanActionDTO.PlanActionEnRetardDTO> listeRetard = enRetard.stream()
                .sorted(Comparator.comparing(PlanActionSuiviRow::getDateFinPrevue))
                .limit(LIMITE_RETARD)
                .map(p -> SuiviGlobalPlanActionDTO.PlanActionEnRetardDTO.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .designation(p.getDesignation())
                        .dateFinPrevue(p.getDateFinPrevue().toString())
                        .joursDeRetard(ChronoUnit.DAYS.between(p.getDateFinPrevue(), aujourdHui))
                        .avancement(p.getAvancement() == null ? BigDecimal.ZERO : p.getAvancement())
                        .responsableLibelle(p.getResponsableId() == null ? null
                                : p.getResponsablePrenom() + " " + p.getResponsableNom())
                        .build())
                .toList();

        // 6. Échéances prochaines (date fin entre aujourd'hui et +30j)
        LocalDate seuilEcheance = aujourdHui.plusDays(SEUIL_ECHEANCE_JOURS);
        List<SuiviGlobalPlanActionDTO.PlanActionEcheanceDTO> echeances = finalPlans.stream()
                .filter(p -> p.getDateFinPrevue() != null
                        && !p.getDateFinPrevue().isBefore(aujourdHui)
                        && !p.getDateFinPrevue().isAfter(seuilEcheance)
                        && !"TERMINEE".equals(p.getStatutCode()))
                .sorted(Comparator.comparing(PlanActionSuiviRow::getDateFinPrevue))
                .limit(LIMITE_ECHEANCES)
                .map(p -> SuiviGlobalPlanActionDTO.PlanActionEcheanceDTO.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .designation(p.getDesignation())
                        .dateFinPrevue(p.getDateFinPrevue().toString())
                        .joursRestants(ChronoUnit.DAYS.between(aujourdHui, p.getDateFinPrevue()))
                        .avancement(p.getAvancement() == null ? BigDecimal.ZERO : p.getAvancement())
                        .responsableLibelle(p.getResponsableId() == null ? null
                                : p.getResponsablePrenom() + " " + p.getResponsableNom())
                        .build())
                .toList();

        // 7. Assemblage
        return SuiviGlobalPlanActionDTO.builder()
                .resume(SuiviGlobalPlanActionDTO.ResumeGlobalDTO.builder()
                        .total(plans.size())
                        .nonCommences(nonCommences)
                        .enCours(enCours)
                        .enRetard(enRetard.size())
                        .termines(termines)
                        .avancementMoyen(moyenne(plans.stream()
                                .map(p -> p.getAvancement() == null
                                        ? BigDecimal.ZERO : p.getAvancement())
                                .toList()))
                        .build())
                .parTypeOrigine(parTypeOrigine)
                .parResponsable(repartitionResponsables)
                .enRetard(listeRetard)
                .echeancesProchaines(echeances)
                .build();
    }

    private BigDecimal moyenne(List<BigDecimal> valeurs) {
        if (valeurs == null || valeurs.isEmpty()) return BigDecimal.ZERO;
        BigDecimal somme = valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return somme.divide(BigDecimal.valueOf(valeurs.size()), 2, RoundingMode.HALF_UP);
    }
}