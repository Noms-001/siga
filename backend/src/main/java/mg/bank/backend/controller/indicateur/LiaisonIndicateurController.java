package mg.bank.backend.controller.indicateur;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.indicateur.IndicateurSimpleDTO;
import mg.bank.backend.service.indicateur.LiaisonIndicateurService;

@RestController
@RequestMapping("/api/activites/{idActivite}/indicateurs")
@RequiredArgsConstructor
public class LiaisonIndicateurController {

    private final LiaisonIndicateurService liaisonIndicateurService;

    /** F8a — lier un indicateur a l'activite. */
    @PostMapping("/{idIndicateur}")
    public ResponseEntity<ApiResponse<IndicateurSimpleDTO>> lier(
            @PathVariable Integer idActivite,
            @PathVariable Integer idIndicateur) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        liaisonIndicateurService.lier(idActivite, idIndicateur)));
    }

    /** F8b — delier un indicateur de l'activite. */
    @DeleteMapping("/{idIndicateur}")
    public ResponseEntity<Void> delier(
            @PathVariable Integer idActivite,
            @PathVariable Integer idIndicateur) {
        liaisonIndicateurService.delier(idActivite, idIndicateur);
        return ResponseEntity.noContent().build();
    }

    /** F8c — indicateurs actifs non encore lies a cette activite. */
    @GetMapping("/disponibles")
    public ResponseEntity<ApiResponse<List<IndicateurSimpleDTO>>> disponibles(
            @PathVariable Integer idActivite,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                liaisonIndicateurService.disponibles(idActivite, search)));
    }
}