package mg.bank.backend.controller.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.dashboard.DashboardAvancementDTO;
import mg.bank.backend.dto.dashboard.DashboardEcheancesDTO;
import mg.bank.backend.dto.dashboard.DashboardKpiDTO;
import mg.bank.backend.dto.dashboard.DashboardPerimetreDTO;
import mg.bank.backend.dto.dashboard.DashboardPlanActionOrigineDTO;
import mg.bank.backend.dto.dashboard.DashboardPtaComparaisonDTO;
import mg.bank.backend.dto.dashboard.DashboardRisquesIncidentsDTO;
import mg.bank.backend.dto.dashboard.DashboardServiceDTO;
import mg.bank.backend.dto.dashboard.DashboardStatutDTO;
import mg.bank.backend.dto.dashboard.DashboardStatutPrioriteDTO;
import mg.bank.backend.dto.dashboard.DashboardTypeActiviteDTO;
import mg.bank.backend.service.common.PeriodeSupport;
import mg.bank.backend.service.common.PeriodeSupport.Periode;
import mg.bank.backend.service.dashboard.DashboardService;

/**
 * API du dashboard SAGA.
 *
 * Un endpoint par famille de KPI, et non un seul /dashboard : c'est le
 * front qui décide quelles sections se rafraîchissent et à quelle
 * fréquence.
 *
 * Le filtre temporel est global : le front envoie dateDebut/dateFin et
 * les applique à tous les endpoints. `annee` reste accepté en repli pour
 * compatibilité. Le périmètre (service / département) est résolu côté
 * service à partir de l'utilisateur authentifié.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/perimetre")
    public ResponseEntity<ApiResponse<DashboardPerimetreDTO>> perimetre() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.perimetre()));
    }

    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<DashboardKpiDTO>> kpi(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "serviceId", required = false) Integer serviceId) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.kpi(periode, serviceId)));
    }

    @GetMapping("/avancement")
    public ResponseEntity<ApiResponse<DashboardAvancementDTO>> avancement(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "serviceId", required = false) Integer serviceId) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.avancement(periode, serviceId)));
    }

    @GetMapping("/statuts")
    public ResponseEntity<ApiResponse<List<DashboardStatutDTO>>> statuts(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "serviceId", required = false) Integer serviceId) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.statuts(periode, serviceId)));
    }

    @GetMapping("/statuts-priorites")
    public ResponseEntity<ApiResponse<List<DashboardStatutPrioriteDTO>>> statutsPriorites(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "serviceId", required = false) Integer serviceId) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.statutsPriorites(periode, serviceId)));
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<DashboardServiceDTO>>> services(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "serviceId", required = false) Integer serviceId) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.services(periode, serviceId)));
    }

    @GetMapping("/echeances")
    public ResponseEntity<ApiResponse<DashboardEcheancesDTO>> echeances(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "jours", required = false) Integer jours,
            @RequestParam(name = "serviceId", required = false) Integer serviceId) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.echeances(periode, jours, serviceId)));
    }

    @GetMapping("/pta")
    public ResponseEntity<ApiResponse<DashboardPtaComparaisonDTO>> pta(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "serviceId", required = false) Integer serviceId) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.pta(periode, serviceId)));
    }

    @GetMapping("/risques-incidents")
    public ResponseEntity<ApiResponse<DashboardRisquesIncidentsDTO>> risquesIncidents(
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.risquesIncidents(annee)));
    }

    @GetMapping("/types-activite")
    public ResponseEntity<ApiResponse<List<DashboardTypeActiviteDTO>>> typesActivite(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee,
            @RequestParam(name = "serviceId", required = false) Integer serviceId) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.typesActivite(periode, serviceId)));
    }

    @GetMapping("/plans-action/avancement-origine")
    public ResponseEntity<ApiResponse<List<DashboardPlanActionOrigineDTO>>> avancementPlansParOrigine(
            @RequestParam(name = "dateDebut", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(name = "dateFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(name = "annee", required = false) Integer annee) {
        Periode periode = PeriodeSupport.resoudre(dateDebut, dateFin, annee);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.avancementPlansParOrigine(periode)));
    }
}
