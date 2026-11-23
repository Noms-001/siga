package mg.bank.backend.controller.affectation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.DecisionDemandeRequest;
import mg.bank.backend.dto.affectation.DemandeRepriseDTO;
import mg.bank.backend.dto.affectation.DemandeRepriseRequest;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.service.affectation.DemandeRepriseService;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/demandes-reprise")
@RequiredArgsConstructor
public class DemandeRepriseController {

    private final DemandeRepriseService demandeRepriseService;

    // ------------------------------------------------------------------
    // Création + historique par activité
    // ------------------------------------------------------------------

    @RequirePermission(ressource = PermissionResource.DEMANDE_REPRISE, action = PermissionAction.CREER)
    @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
    @PostMapping("/activites/{idActivite}")
    public ResponseEntity<ApiResponse<DemandeRepriseDTO>> creer(
            @PathVariable Integer idActivite,
            @Valid @RequestBody(required = false) DemandeRepriseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        demandeRepriseService.creer(idActivite, request)));
    }

    @RequirePermission(ressource = PermissionResource.DEMANDE_REPRISE, action = PermissionAction.CONSULTER)
    @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
    @GetMapping("/activites/{idActivite}")
    public ResponseEntity<ApiResponse<List<DemandeRepriseDTO>>> historiqueActivite(
            @PathVariable Integer idActivite) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.historiqueActivite(idActivite)));
    }

    // ------------------------------------------------------------------
    // Vues personnelles
    // ------------------------------------------------------------------

    @RequirePermission(ressource = PermissionResource.DEMANDE_REPRISE, action = PermissionAction.CONSULTER)
    @GetMapping("/mes-demandes")
    public ResponseEntity<ApiResponse<List<DemandeRepriseDTO>>> mesDemandes() {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.mesDemandes()));
    }

    @RequirePermission(ressource = PermissionResource.DEMANDE_REPRISE, action = PermissionAction.CONSULTER)
    @GetMapping("/a-traiter")
    public ResponseEntity<ApiResponse<List<DemandeRepriseDTO>>> aTraiter() {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.aTraiter()));
    }

    // ------------------------------------------------------------------
    // Décisions
    // ------------------------------------------------------------------

    @RequirePermission(ressource = PermissionResource.DEMANDE_REPRISE, action = PermissionAction.VALIDER)
    @PostMapping("/{id}/accepter")
    public ResponseEntity<ApiResponse<DemandeRepriseDTO>> accepter(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) DecisionDemandeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.accepter(id, request)));
    }

    @RequirePermission(ressource = PermissionResource.DEMANDE_REPRISE, action = PermissionAction.VALIDER)
    @PostMapping("/{id}/refuser")
    public ResponseEntity<ApiResponse<DemandeRepriseDTO>> refuser(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) DecisionDemandeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.refuser(id, request)));
    }

    @RequirePermission(ressource = PermissionResource.DEMANDE_REPRISE, action = PermissionAction.MODIFIER)
    @PostMapping("/{id}/annuler")
    public ResponseEntity<ApiResponse<DemandeRepriseDTO>> annuler(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.annuler(id)));
    }
}
