package mg.bank.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.dashboard.DashboardAvancementDTO;
import mg.bank.backend.dto.dashboard.DashboardEcheanceDTO;
import mg.bank.backend.dto.dashboard.DashboardEcheancesDTO;
import mg.bank.backend.dto.dashboard.DashboardKpiDTO;
import mg.bank.backend.dto.dashboard.DashboardPlansActionDTO;
import mg.bank.backend.dto.dashboard.DashboardPrioriteDTO;
import mg.bank.backend.dto.dashboard.DashboardPtaComparaisonDTO;
import mg.bank.backend.dto.dashboard.DashboardPtaDTO;
import mg.bank.backend.dto.dashboard.DashboardRisquesIncidentsDTO;
import mg.bank.backend.dto.dashboard.DashboardServiceDTO;
import mg.bank.backend.dto.dashboard.DashboardStatutDTO;
import mg.bank.backend.dto.dashboard.DashboardValidationDTO;
import mg.bank.backend.enums.StatutActiviteEnum;
import mg.bank.backend.repository.DashboardRepository;
import mg.bank.backend.repository.projection.DashboardAvancementRow;
import mg.bank.backend.repository.projection.DashboardEcheanceRow;
import mg.bank.backend.repository.projection.DashboardKpiRow;
import mg.bank.backend.repository.projection.DashboardPlanActionRow;
import mg.bank.backend.repository.projection.DashboardPtaRow;
import mg.bank.backend.repository.projection.DashboardValidationRow;

/**
 * Agrégats du dashboard.
 *
 * Ce service ne prend AUCUNE décision métier : il prépare les paramètres
 * (codes terminaux, année, limite), appelle le repository, et convertit
 * les projections en DTO. Toute la logique de calcul est dans les
 * requêtes SQL — c'est le sens même d'un dashboard : des agrégats, pas
 * des boucles Java.
 *
 * Pas encore de filtre par poste ou par périmètre : la version actuelle
 * renvoie les statistiques globales pour l'année demandée. L'ajout du
 * périmètre se fera en une seule modification de ce service (un paramètre
 * à faire descendre dans chaque méthode), sans toucher au controller ni
 * aux DTO.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Limite par défaut pour la liste des activités prioritaires. */
    private static final int LIMITE_PRIORITES_DEFAUT = 10;
    private static final int LIMITE_PRIORITES_MAX = 100;

    /** Fenêtre par défaut pour les échéances proches, en jours. */
    private static final int JOURS_ECHEANCE_DEFAUT = 7;
    private static final int JOURS_ECHEANCE_MAX = 365;

    private final DashboardRepository dashboardRepository;

    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public DashboardKpiDTO kpi(Integer annee) {
        DashboardKpiRow row = dashboardRepository.kpi(annee, codesTerminaux());
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
    public DashboardAvancementDTO avancement(Integer annee) {
        DashboardAvancementRow row = dashboardRepository.avancement(annee);
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
    public List<DashboardStatutDTO> statuts(Integer annee) {
        return dashboardRepository.statuts(annee).stream()
                .map(r -> DashboardStatutDTO.builder()
                        .statut(r.getStatut())
                        .libelle(r.getLibelle())
                        .nombre(nulSiAbsent(r.getNombre()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardServiceDTO> services(Integer annee) {
        return dashboardRepository.services(annee, codesTerminaux()).stream()
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
    public List<DashboardPrioriteDTO> priorites(Integer annee, Integer limit) {
        int limite = normaliserLimite(limit);
        return dashboardRepository.priorites(annee, codesTerminaux(), limite).stream()
                .map(r -> DashboardPrioriteDTO.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .reference(r.getReference())
                        .designation(r.getDesignation())
                        .service(r.getService())
                        .priorite(r.getPrioriteLibelle())
                        .prioriteCode(r.getPrioriteCode())
                        .statut(r.getStatutCode())
                        .statutLibelle(r.getStatutLibelle())
                        .dateDebut(r.getDateDebut())
                        .dateEcheance(r.getDateEcheance())
                        .avancement(nulSiAbsentDouble(r.getAvancement()))
                        .joursRetard(nulSiAbsent(r.getJoursRetard()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardValidationDTO> validations(Integer annee) {
        return dashboardRepository.validationsEnAttente(annee).stream()
                .map(this::versValidation)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardEcheancesDTO echeances(Integer annee, Integer jours) {
        int fenetre = normaliserFenetre(jours);

        List<DashboardEcheanceRow> toutes = dashboardRepository.echeances(
                annee, codesTerminaux(), fenetre);

        /*
         * Séparation en mémoire : deux listes à partir d'une requête.
         * Un split sur le signe de `jours` — négatif = retard, positif =
         * proche. Le zéro (échéance aujourd'hui) est compté en retard,
         * c'est la convention la plus stricte : "arrive aujourd'hui sans
         * avoir commencé" est déjà un problème.
         */
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
    public DashboardPtaComparaisonDTO pta(Integer annee) {
        return DashboardPtaComparaisonDTO.builder()
                .pta(versPta(dashboardRepository.pta(annee, true, codesTerminaux())))
                .nonPta(versPta(dashboardRepository.pta(annee, false, codesTerminaux())))
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardRisquesIncidentsDTO risquesIncidents(Integer annee) {
        DashboardPlanActionRow row = dashboardRepository.plansAction(annee);
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

    /* ------------------------------------------------------------------ */

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

    private DashboardValidationDTO versValidation(DashboardValidationRow r) {
        String demandeur = composerNom(r.getDemandeurPrenom(), r.getDemandeurNom());
        return DashboardValidationDTO.builder()
                .idValidation(r.getIdValidation())
                .idActivite(r.getIdActivite())
                .codeActivite(r.getCodeActivite())
                .designationActivite(r.getDesignationActivite())
                .demandeur(demandeur)
                .etape(r.getEtapeDesignation())
                .niveau(r.getEtapeNiveau())
                .dateDemande(r.getDateDemande())
                .decision(r.getDecision())
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
        return String.join(",", StatutActiviteEnum.codesTerminaux());
    }

    private int normaliserLimite(Integer limit) {
        if (limit == null || limit < 1)
            return LIMITE_PRIORITES_DEFAUT;
        return Math.min(limit, LIMITE_PRIORITES_MAX);
    }

    private int normaliserFenetre(Integer jours) {
        if (jours == null || jours < 1)
            return JOURS_ECHEANCE_DEFAUT;
        return Math.min(jours, JOURS_ECHEANCE_MAX);
    }

    private String composerNom(String prenom, String nom) {
        if (prenom == null && nom == null)
            return null;
        if (prenom == null)
            return nom;
        if (nom == null)
            return prenom;
        return prenom + " " + nom;
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