package mg.bank.backend.service.dashboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.OptionDTO;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.dashboard.DashboardAvancementDTO;
import mg.bank.backend.dto.dashboard.DashboardEcheanceDTO;
import mg.bank.backend.dto.dashboard.DashboardEcheancesDTO;
import mg.bank.backend.dto.dashboard.DashboardKpiDTO;
import mg.bank.backend.dto.dashboard.DashboardPerimetreDTO;
import mg.bank.backend.dto.dashboard.DashboardPlanActionOrigineDTO;
import mg.bank.backend.dto.dashboard.DashboardPlansActionDTO;
import mg.bank.backend.dto.dashboard.DashboardPtaComparaisonDTO;
import mg.bank.backend.dto.dashboard.DashboardPtaDTO;
import mg.bank.backend.dto.dashboard.DashboardRisquesIncidentsDTO;
import mg.bank.backend.dto.dashboard.DashboardServiceDTO;
import mg.bank.backend.dto.dashboard.DashboardStatutDTO;
import mg.bank.backend.dto.dashboard.DashboardStatutPrioriteDTO;
import mg.bank.backend.dto.dashboard.DashboardTypeActiviteDTO;
import mg.bank.backend.enums.StatutActiviteEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.repository.dashboard.DashboardRepository;
import mg.bank.backend.repository.projection.dashboard.DashboardAvancementRow;
import mg.bank.backend.repository.projection.dashboard.DashboardEcheanceRow;
import mg.bank.backend.repository.projection.dashboard.DashboardKpiRow;
import mg.bank.backend.repository.projection.dashboard.DashboardPlanActionOrigineRow;
import mg.bank.backend.repository.projection.dashboard.DashboardPlanActionRow;
import mg.bank.backend.repository.projection.dashboard.DashboardPtaRow;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.service.common.ActiviteQuerySupport;
import mg.bank.backend.service.common.PeriodeSupport.Periode;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Fenêtre par défaut pour les échéances proches, en jours. */
    private static final int JOURS_ECHEANCE_DEFAUT = 7;
    private static final int JOURS_ECHEANCE_MAX = 365;

    private static final List<String> ORIGINES =
            List.of("INCIDENT", "RISQUE", "AUTRE");
    private static final Map<String, String> LIBELLES_ORIGINE = Map.of(
            "INCIDENT", "Incident",
            "RISQUE", "Risque",
            "AUTRE", "Autre");

    private final DashboardRepository dashboardRepository;
    private final CurrentUserService currentUserService;
    private final ServiceRepository serviceRepository;

    /** Filtres de perimetre transmis aux requetes SQL. */
    private record Scope(Integer service, Integer departement, Integer idUtilisateurPortee) {
    }

    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public DashboardKpiDTO kpi(Periode periode, Integer serviceId) {
        Scope scope = resoudreScope(serviceId);
        DashboardKpiRow row = dashboardRepository.kpi(
                periode.debut(), periode.fin(), periode.reference(),
                scope.service(), scope.departement(), scope.idUtilisateurPortee(), codesTerminaux());
        if (row == null) {
            return DashboardKpiDTO.builder().build();
        }
        return DashboardKpiDTO.builder()
                .totalActivites(nulSiAbsent(row.getTotal()))
                .enCours(nulSiAbsent(row.getEnCours()))
                .terminees(nulSiAbsent(row.getTerminees()))
                .enRetard(nulSiAbsent(row.getEnRetard()))
                .nonCommencees(nulSiAbsent(row.getNonCommencees()))
                .suspendues(nulSiAbsent(row.getSuspendues()))
                .annulees(nulSiAbsent(row.getAnnulees()))
                .reportees(nulSiAbsent(row.getReportees()))
                .enAttenteValidation(nulSiAbsent(row.getEnAttenteValidation()))
                .avancementGlobal(nulSiAbsentDouble(row.getAvancementGlobal()))
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardAvancementDTO avancement(Periode periode, Integer serviceId) {
        Scope scope = resoudreScope(serviceId);
        DashboardAvancementRow row = dashboardRepository.avancement(
                periode.debut(), periode.fin(), periode.reference(),
                scope.service(), scope.departement(), scope.idUtilisateurPortee());
        if (row == null) {
            return DashboardAvancementDTO.builder().build();
        }
        return DashboardAvancementDTO.builder()
                .avancementGlobal(nulSiAbsentDouble(row.getAvancementGlobal()))
                .totalActivites(nulSiAbsent(row.getTotalActivites()))
                .totalSousActivites(nulSiAbsent(row.getTotalSousActivites()))
                .sousActivitesTerminees(nulSiAbsent(row.getSousActivitesTerminees()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<DashboardStatutDTO> statuts(Periode periode, Integer serviceId) {
        Scope scope = resoudreScope(serviceId);
        return dashboardRepository.statuts(
                periode.debut(), periode.fin(), periode.reference(),
                scope.service(), scope.departement(), scope.idUtilisateurPortee()).stream()
                .map(r -> DashboardStatutDTO.builder()
                        .statut(r.getStatut())
                        .libelle(r.getLibelle())
                        .nombre(nulSiAbsent(r.getNombre()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardStatutPrioriteDTO> statutsPriorites(Periode periode, Integer serviceId) {
        Scope scope = resoudreScope(serviceId);
        return dashboardRepository.statutsPriorites(
                periode.debut(), periode.fin(), periode.reference(),
                scope.service(), scope.departement(), scope.idUtilisateurPortee()).stream()
                .map(r -> DashboardStatutPrioriteDTO.builder()
                        .statut(r.getStatut())
                        .statutLibelle(r.getStatutLibelle())
                        .priorite(r.getPriorite())
                        .prioriteLibelle(r.getPrioriteLibelle())
                        .nombre(nulSiAbsent(r.getNombre()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardServiceDTO> services(Periode periode, Integer serviceId) {
        Scope scope = resoudreScope(serviceId);
        return dashboardRepository.services(
                periode.debut(), periode.fin(), periode.reference(),
                scope.service(), scope.departement(), scope.idUtilisateurPortee(), codesTerminaux()).stream()
                .map(r -> DashboardServiceDTO.builder()
                        .idService(r.getIdService())
                        .service(r.getService())
                        .totalActivites(nulSiAbsent(r.getTotalActivites()))
                        .terminees(nulSiAbsent(r.getTerminees()))
                        .enCours(nulSiAbsent(r.getEnCours()))
                        .enRetard(nulSiAbsent(r.getEnRetard()))
                        .nonCommencees(nulSiAbsent(r.getNonCommencees()))
                        .avancement(nulSiAbsentDouble(r.getAvancement()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardTypeActiviteDTO> typesActivite(Periode periode, Integer serviceId) {
        Scope scope = resoudreScope(serviceId);
        return dashboardRepository.typesActivite(
                periode.debut(), periode.fin(), periode.reference(),
                scope.service(), scope.departement(), scope.idUtilisateurPortee()).stream()
                .map(r -> DashboardTypeActiviteDTO.builder()
                        .idTypeActivite(r.getIdTypeActivite())
                        .typeActivite(r.getTypeActivite())
                        .totalActivites(nulSiAbsent(r.getTotalActivites()))
                        .avancement(nulSiAbsentDouble(r.getAvancement()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardEcheancesDTO echeances(Periode periode, Integer jours, Integer serviceId) {
        int fenetre = normaliserFenetre(jours);
        Scope scope = resoudreScope(serviceId);

        List<DashboardEcheanceRow> toutes = dashboardRepository.echeances(
                periode.debut(), periode.fin(), periode.reference(),
                scope.service(), scope.departement(), scope.idUtilisateurPortee(), codesTerminaux(), fenetre);

        List<DashboardEcheanceDTO> enRetard = toutes.stream()
                .filter(r -> r.getJours() != null && r.getJours() > 0) // ← strictement
                .map(this::versEcheance)
                .toList();

        List<DashboardEcheanceDTO> proches = toutes.stream()
                .filter(r -> r.getJours() != null && r.getJours() <= 0) // ← inclut aujourd'hui
                .map(this::versEcheance)
                .toList();

        return DashboardEcheancesDTO.builder()
                .enRetard(enRetard)
                .echeanceProche(proches)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardPtaComparaisonDTO pta(Periode periode, Integer serviceId) {
        Scope scope = resoudreScope(serviceId);
        return DashboardPtaComparaisonDTO.builder()
                .pta(versPta(dashboardRepository.pta(
                        periode.debut(), periode.fin(), periode.reference(),
                        scope.service(), scope.departement(), scope.idUtilisateurPortee(), true, codesTerminaux())))
                .nonPta(versPta(dashboardRepository.pta(
                        periode.debut(), periode.fin(), periode.reference(),
                        scope.service(), scope.departement(), scope.idUtilisateurPortee(), false, codesTerminaux())))
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardRisquesIncidentsDTO risquesIncidents(Integer annee) {
        Scope scope = resoudreScope(null);
        DashboardPlanActionRow row = dashboardRepository.plansAction(
                annee, scope.service(), scope.departement(), scope.idUtilisateurPortee());
        DashboardPlansActionDTO plans = row == null
                ? DashboardPlansActionDTO.builder().build()
                : DashboardPlansActionDTO.builder()
                        .total(nulSiAbsent(row.getTotal()))
                        .enCours(nulSiAbsent(row.getEnCours()))
                        .termines(nulSiAbsent(row.getTermines()))
                        .enRetard(nulSiAbsent(row.getEnRetard()))
                        .build();

        /*
         * Risques et incidents ne sont pas exposés : ils n'ont pas de table
         * dans le modèle SAGA. Le DTO ne les porte volontairement pas pour
         * éviter un "0" ambigu qui laisserait croire à une absence de
         * risque plutôt qu'à une absence de module.
         */
        return DashboardRisquesIncidentsDTO.builder()
                .plansAction(plans)
                .build();
    }

    /**
     * Avancement moyen des plans d'action par origine sur la période.
     * Les plans sont restreints au périmètre de la permission courante.
     */
    @Transactional(readOnly = true)
    public List<DashboardPlanActionOrigineDTO> avancementPlansParOrigine(Periode periode) {
        Scope scope = resoudreScope(null);
        Map<String, DashboardPlanActionOrigineRow> index = new HashMap<>();
        for (DashboardPlanActionOrigineRow row
                : dashboardRepository.avancementPlansParOrigine(
                        periode.debut(), periode.fin(), periode.reference(),
                        scope.service(), scope.departement(), scope.idUtilisateurPortee())) {
            if (row.getOrigine() == null) {
                continue;
            }
            index.put(row.getOrigine(), row);
        }

        return ORIGINES.stream()
                .map(origine -> {
                    DashboardPlanActionOrigineRow row = index.get(origine);
                    long total = row == null ? 0 : nulSiAbsent(row.getTotal());
                    double avancement = row == null
                            ? 0
                            : arrondi(nulSiAbsentDouble(row.getAvancement()));
                    return DashboardPlanActionOrigineDTO.builder()
                            .origine(origine)
                            .libelle(LIBELLES_ORIGINE.get(origine))
                            .total(total)
                            .avancement(avancement)
                            .build();
                })
                .toList();
    }

    /**
     * Périmètre de l'utilisateur connecté et services proposés au filtre
     * du graphique « types d'activité ».
     */
    @Transactional(readOnly = true)
    public DashboardPerimetreDTO perimetre() {
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

        if (perimetre.estRattacheService()) {
            String nom = serviceRepository.findById(perimetre.getIdService())
                    .map(s -> s.getNom())
                    .orElse(null);
            return DashboardPerimetreDTO.builder()
                    .niveau("SERVICE")
                    .idService(perimetre.getIdService())
                    .idDepartement(perimetre.getIdDepartement())
                    .service(nom)
                    .services(List.of())
                    .build();
        }

        if (perimetre.estNiveauDepartement()) {
            List<OptionDTO> services = ActiviteQuerySupport.versOptions(
                    serviceRepository.findOptionsActivesParDepartement(
                            perimetre.getIdDepartement()));
            return DashboardPerimetreDTO.builder()
                    .niveau("DEPARTEMENT")
                    .idService(null)
                    .idDepartement(perimetre.getIdDepartement())
                    .service(null)
                    .services(services)
                    .build();
        }

        return DashboardPerimetreDTO.builder()
                .niveau("GLOBAL")
                .idService(null)
                .idDepartement(null)
                .service(null)
                .services(List.of())
                .build();
    }

    /* ------------------------------------------------------------------ */

    /**
     * Traduit le périmètre de l'utilisateur et un éventuel service demandé
     * en filtres SQL. Un utilisateur rattaché à un service ne peut jamais
     * élargir son périmètre ; un chef de département ne peut interroger
     * qu'un service de son département.
     */
    private Scope resoudreScope(Integer serviceIdDemande) {
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

        if (perimetre.estRattacheService()) {
            return new Scope(perimetre.getIdService(), null, perimetre.getIdUtilisateurPortee());
        }

        if (perimetre.estNiveauDepartement()) {
            if (serviceIdDemande != null
                    && !serviceDuDepartement(serviceIdDemande, perimetre.getIdDepartement())) {
                throw new ApiException(
                        "Service hors de votre périmètre",
                        HttpStatus.FORBIDDEN);
            }
            return new Scope(serviceIdDemande, perimetre.getIdDepartement(),
                    perimetre.getIdUtilisateurPortee());
        }

        return new Scope(serviceIdDemande, null, perimetre.getIdUtilisateurPortee());
    }

    private boolean serviceDuDepartement(Integer idService, Integer idDepartement) {
        return serviceRepository.findById(idService)
                .map(s -> s.getDepartement() != null
                        && idDepartement.equals(s.getDepartement().getIdDepartement()))
                .orElse(false);
    }

    private double arrondi(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }

    private DashboardPtaDTO versPta(DashboardPtaRow row) {
        if (row == null)
            return DashboardPtaDTO.builder().build();
        return DashboardPtaDTO.builder()
                .total(nulSiAbsent(row.getTotal()))
                .terminees(nulSiAbsent(row.getTerminees()))
                .enCours(nulSiAbsent(row.getEnCours()))
                .enRetard(nulSiAbsent(row.getEnRetard()))
                .nonCommencees(nulSiAbsent(row.getNonCommencees()))
                .avancement(nulSiAbsentDouble(row.getAvancement()))
                .build();
    }

    private DashboardEcheanceDTO versEcheance(DashboardEcheanceRow r) {
        return DashboardEcheanceDTO.builder()
                .id(r.getId())
                .code(r.getCode())
                .designation(r.getDesignation())
                .service(r.getService())
                .dateEcheance(r.getDateEcheance())
                // Le signe porte le sens : >=0 = en retard, <0 = à venir.
                .jours(r.getJours() == null ? 0L : r.getJours())
                .priorite(r.getPrioriteLibelle())
                .prioriteCode(r.getPrioriteCode())
                .statut(r.getStatutCode())
                .statutLibelle(r.getStatutLibelle())
                .avancement(nulSiAbsentDouble(r.getAvancement()))
                .build();
    }

    private String codesTerminaux() {
        return StatutActiviteEnum.codesTerminauxSql();
    }

    private int normaliserFenetre(Integer jours) {
        if (jours == null || jours < 1)
            return JOURS_ECHEANCE_DEFAUT;
        return Math.min(jours, JOURS_ECHEANCE_MAX);
    }

    private long nulSiAbsent(Long v) {
        return v == null ? 0L : v;
    }

    private long nulSiAbsent(Integer v) {
        return v == null ? 0L : v.longValue();
    }

    private double nulSiAbsentDouble(Double v) {
        return v == null ? 0d : v;
    }
}
