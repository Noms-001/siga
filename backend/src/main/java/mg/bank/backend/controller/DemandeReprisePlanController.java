package mg.bank.backend.controller;

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
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.DecisionDemandeRequest;
import mg.bank.backend.dto.planaction.DemandeReprisePlanDTO;
import mg.bank.backend.dto.DemandeRepriseRequest;
import mg.bank.backend.service.DemandeReprisePlanService;

@RestController
@RequestMapping("/api/demandes-reprise-plans")
@RequiredArgsConstructor
public class DemandeReprisePlanController {

    private final DemandeReprisePlanService demandeReprisePlanService;

    @PostMapping("/plans/{idPlanAction}")
    public ResponseEntity<ApiResponse<DemandeReprisePlanDTO>> creer(
            @PathVariable Integer idPlanAction,
            @Valid @RequestBody(required = false) DemandeRepriseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        demandeReprisePlanService.creer(idPlanAction, request)));
    }

    @GetMapping("/plans/{idPlanAction}")
    public ResponseEntity<ApiResponse<List<DemandeReprisePlanDTO>>> historiquePlan(
            @PathVariable Integer idPlanAction) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeReprisePlanService.historiquePlan(idPlanAction)));
    }

    @GetMapping("/mes-demandes")
    public ResponseEntity<ApiResponse<List<DemandeReprisePlanDTO>>> mesDemandes() {
        return ResponseEntity.ok(ApiResponse.success(
                demandeReprisePlanService.mesDemandes()));
    }

    @GetMapping("/a-traiter")
    public ResponseEntity<ApiResponse<List<DemandeReprisePlanDTO>>> aTraiter() {
        return ResponseEntity.ok(ApiResponse.success(
                demandeReprisePlanService.aTraiter()));
    }

    @PostMapping("/{id}/accepter")
    public ResponseEntity<ApiResponse<DemandeReprisePlanDTO>> accepter(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) DecisionDemandeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeReprisePlanService.accepter(id, request)));
    }

    @PostMapping("/{id}/refuser")
    public ResponseEntity<ApiResponse<DemandeReprisePlanDTO>> refuser(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) DecisionDemandeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeReprisePlanService.refuser(id, request)));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<ApiResponse<DemandeReprisePlanDTO>> annuler(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeReprisePlanService.annuler(id)));
    }
}