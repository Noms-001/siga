package mg.bank.backend.controller.backoffice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.backoffice.IndicateurFiltresResponse;
import mg.bank.backend.dto.backoffice.IndicateurRequest;
import mg.bank.backend.dto.backoffice.IndicateurResponse;
import mg.bank.backend.service.IndicateurService;

@RestController
@RequestMapping("/api/backoffice/indicateurs")
@RequiredArgsConstructor
public class IndicateurController {

    private final IndicateurService indicateurService;

    /* -------------------- lecture -------------------- */

    /**
     * Liste filtrée.
     *
     * Tous les paramètres sont optionnels. `actif` absent → tous les
     * indicateurs ; `search` porte sur code + désignation ; `type` est
     * un filtre exact.
     *
     * Route `/filtres` déclarée AVANT `/{id}` pour qu'elle ne soit pas
     * capturée comme un identifiant.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<IndicateurResponse>>> lister(
            @RequestParam(name = "actif", required = false) Boolean actif,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(
                ApiResponse.success(indicateurService.lister(actif, type, search)));
    }

    @GetMapping("/filtres")
    public ResponseEntity<ApiResponse<IndicateurFiltresResponse>> getFiltres() {
        return ResponseEntity.ok(
                ApiResponse.success(indicateurService.getFiltres()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IndicateurResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(indicateurService.getById(id)));
    }

    /* -------------------- écriture -------------------- */

    @PostMapping
    public ResponseEntity<ApiResponse<IndicateurResponse>> creer(
            @Valid @RequestBody IndicateurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(indicateurService.creer(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IndicateurResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody IndicateurRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(indicateurService.modifier(id, request)));
    }

    /* -------------------- bascule -------------------- */

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<IndicateurResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(indicateurService.desactiver(id)));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<IndicateurResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(indicateurService.activer(id)));
    }
}