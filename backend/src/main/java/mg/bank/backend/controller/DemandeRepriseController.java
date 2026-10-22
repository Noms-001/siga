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
import mg.bank.backend.dto.DemandeRepriseDTO;
import mg.bank.backend.dto.DemandeRepriseRequest;
import mg.bank.backend.service.DemandeRepriseService;

@RestController
@RequestMapping("/api/demandes-reprise")
@RequiredArgsConstructor
public class DemandeRepriseController {

    private final DemandeRepriseService demandeRepriseService;

    // ------------------------------------------------------------------
    // Création + historique par activité
    // ------------------------------------------------------------------

    @PostMapping("/activites/{idActivite}")
    public ResponseEntity<ApiResponse<DemandeRepriseDTO>> creer(
            @PathVariable Integer idActivite,
            @Valid @RequestBody(required = false) DemandeRepriseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        demandeRepriseService.creer(idActivite, request)));
    }

    @GetMapping("/activites/{idActivite}")
    public ResponseEntity<ApiResponse<List<DemandeRepriseDTO>>> historiqueActivite(
            @PathVariable Integer idActivite) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.historiqueActivite(idActivite)));
    }

    // ------------------------------------------------------------------
    // Vues personnelles
    // ------------------------------------------------------------------

    @GetMapping("/mes-demandes")
    public ResponseEntity<ApiResponse<List<DemandeRepriseDTO>>> mesDemandes() {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.mesDemandes()));
    }

    @GetMapping("/a-traiter")
    public ResponseEntity<ApiResponse<List<DemandeRepriseDTO>>> aTraiter() {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.aTraiter()));
    }

    // ------------------------------------------------------------------
    // Décisions
    // ------------------------------------------------------------------

    @PostMapping("/{id}/accepter")
    public ResponseEntity<ApiResponse<DemandeRepriseDTO>> accepter(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) DecisionDemandeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.accepter(id, request)));
    }

    @PostMapping("/{id}/refuser")
    public ResponseEntity<ApiResponse<DemandeRepriseDTO>> refuser(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) DecisionDemandeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.refuser(id, request)));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<ApiResponse<DemandeRepriseDTO>> annuler(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeRepriseService.annuler(id)));
    }
}