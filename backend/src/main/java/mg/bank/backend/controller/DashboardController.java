package mg.bank.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.dashboard.DashboardAvancementDTO;
import mg.bank.backend.dto.dashboard.DashboardEcheancesDTO;
import mg.bank.backend.dto.dashboard.DashboardKpiDTO;
import mg.bank.backend.dto.dashboard.DashboardPrioriteDTO;
import mg.bank.backend.dto.dashboard.DashboardPtaComparaisonDTO;
import mg.bank.backend.dto.dashboard.DashboardRisquesIncidentsDTO;
import mg.bank.backend.dto.dashboard.DashboardServiceDTO;
import mg.bank.backend.dto.dashboard.DashboardStatutDTO;
import mg.bank.backend.dto.dashboard.DashboardValidationDTO;
import mg.bank.backend.service.DashboardService;

/**
 * API du dashboard SAGA.
 *
 * Un endpoint par famille de KPI, et non un seul /dashboard : c'est le
 * front qui décide quelles sections se rafraîchissent et à quelle
 * fréquence. Un endpoint unique obligerait à tout recalculer à chaque
 * appel, y compris les blocs non affichés.
 *
 * Le paramètre `annee` est facultatif partout. Sans lui, les agrégats
 * couvrent toutes les années — c'est cohérent avec /api/activites où
 * omettre `annee` équivaut à ne pas filtrer.
 *
 * Version 1 : pas de filtre par poste, pas de permissions. Les agrégats
 * sont globaux pour l'année demandée. Le périmètre sera ajouté plus tard,
 * sans changer les signatures — l'appelant d'alors recevra un
 * PerimetreUtilisateur en paramètre supplémentaire.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<DashboardKpiDTO>> kpi(
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.kpi(annee)));
    }

    @GetMapping("/avancement")
    public ResponseEntity<ApiResponse<DashboardAvancementDTO>> avancement(
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.avancement(annee)));
    }

    @GetMapping("/statuts")
    public ResponseEntity<ApiResponse<List<DashboardStatutDTO>>> statuts(
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.statuts(annee)));
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<DashboardServiceDTO>>> services(
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.services(annee)));
    }

    @GetMapping("/priorites")
    public ResponseEntity<ApiResponse<List<DashboardPrioriteDTO>>> priorites(
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.priorites(annee, limit)));
    }

    @GetMapping("/validations")
    public ResponseEntity<ApiResponse<List<DashboardValidationDTO>>> validations(
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.validations(annee)));
    }

    @GetMapping("/echeances")
    public ResponseEntity<ApiResponse<DashboardEcheancesDTO>> echeances(
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "jours", required = false) Integer jours) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.echeances(annee, jours)));
    }

    @GetMapping("/pta")
    public ResponseEntity<ApiResponse<DashboardPtaComparaisonDTO>> pta(
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.pta(annee)));
    }

    @GetMapping("/risques-incidents")
    public ResponseEntity<ApiResponse<DashboardRisquesIncidentsDTO>> risquesIncidents(
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.risquesIncidents(annee)));
    }
}