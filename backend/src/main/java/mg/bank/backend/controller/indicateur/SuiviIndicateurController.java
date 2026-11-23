package mg.bank.backend.controller.indicateur;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.indicateur.AgregationIndicateurDTO;
import mg.bank.backend.dto.indicateur.AtteinteIndicateurDTO;
import mg.bank.backend.dto.indicateur.CalendrierIndicateurDTO;
import mg.bank.backend.dto.indicateur.DashboardIndicateurDTO;
import mg.bank.backend.dto.indicateur.IndicateurEnRetardDTO;
import mg.bank.backend.dto.indicateur.SuiviIndicateurDTO;
import mg.bank.backend.dto.indicateur.ValeurSuiviDTO;
import mg.bank.backend.dto.indicateur.ValeurIndicateurRequest;
import mg.bank.backend.enums.MethodeAgregation;
import mg.bank.backend.service.indicateur.SuiviIndicateurService;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/indicateurs")
@RequiredArgsConstructor
public class SuiviIndicateurController {

        private final SuiviIndicateurService suiviIndicateurService;

        @RequirePermission(ressource = PermissionResource.VALEUR_INDICATEUR, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.CONSULTER)
        @GetMapping("/{id}/valeurs")
        public ResponseEntity<ApiResponse<Page<ValeurSuiviDTO>>> historique(
                        @PathVariable Integer id,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                return ResponseEntity.ok(
                                ApiResponse.success(suiviIndicateurService.historique(id, page, size)));
        }

        @RequirePermission(ressource = PermissionResource.VALEUR_INDICATEUR, action = PermissionAction.CREER)
        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.MODIFIER)
        @PostMapping("/{id}/valeurs")
        public ResponseEntity<ApiResponse<ValeurSuiviDTO>> saisir(
                        @PathVariable Integer id,
                        @Valid @RequestBody ValeurIndicateurRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(suiviIndicateurService.saisir(id, request)));
        }

        @RequirePermission(ressource = PermissionResource.VALEUR_INDICATEUR, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.CONSULTER)
        @GetMapping("/{id}/valeur-courante")
        public ResponseEntity<ApiResponse<ValeurSuiviDTO>> valeurCourante(
                        @PathVariable Integer id) {
                return ResponseEntity.ok(
                                ApiResponse.success(suiviIndicateurService.valeurCourante(id)));
        }

        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.CONSULTER)
        @GetMapping("/{id}/atteinte")
        public ResponseEntity<ApiResponse<AtteinteIndicateurDTO>> atteinte(
                        @PathVariable Integer id) {
                return ResponseEntity.ok(
                                ApiResponse.success(suiviIndicateurService.atteinte(id)));
        }

        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/activites/{idActivite}/suivi")
        public ResponseEntity<ApiResponse<SuiviIndicateurDTO>> suiviActivite(
                        @PathVariable Integer idActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(suiviIndicateurService.suiviActivite(idActivite)));
        }

        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.CONSULTER)
        @GetMapping("/{id}/agregation")
        public ResponseEntity<ApiResponse<AgregationIndicateurDTO>> agregation(
                        @PathVariable Integer id,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
                        @RequestParam MethodeAgregation methode) {
                return ResponseEntity.ok(ApiResponse.success(
                                suiviIndicateurService.agreger(id, debut, fin, methode)));
        }

        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.CONSULTER)
        @GetMapping("/en-retard")
        public ResponseEntity<ApiResponse<List<IndicateurEnRetardDTO>>> enRetard(
                        @RequestParam(defaultValue = "false") boolean inclureAJour,
                        @RequestParam(defaultValue = "true") boolean inclureJamaisSaisi) {
                return ResponseEntity.ok(ApiResponse.success(
                                suiviIndicateurService.listerEnRetard(inclureAJour, inclureJamaisSaisi)));
        }

        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.CONSULTER)
        @GetMapping("/{id}/calendrier")
        public ResponseEntity<ApiResponse<CalendrierIndicateurDTO>> calendrier(
                        @PathVariable Integer id,
                        @RequestParam(required = false) Integer annee) {
                return ResponseEntity.ok(ApiResponse.success(
                                suiviIndicateurService.calendrier(id, annee)));
        }

        @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.DASHBOARD, action = PermissionAction.CONSULTER)
        @GetMapping("/dashboard")
        public ResponseEntity<ApiResponse<DashboardIndicateurDTO>> dashboard() {
                return ResponseEntity.ok(ApiResponse.success(
                                suiviIndicateurService.dashboard()));
        }
}
