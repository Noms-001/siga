package mg.bank.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.AvancementRequest;
import mg.bank.backend.dto.AvancementResponse;
import mg.bank.backend.service.SousActiviteService;

@RestController
@RequestMapping("/api/sous-activites")
@RequiredArgsConstructor
public class SousActiviteController {

    private final SousActiviteService sousActiviteService;

    /**
     * Enregistre un nouvel avancement pour une sous-activité.
     *
     * POST et non PUT/PATCH : on AJOUTE un relevé à l'historique, on ne
     * modifie pas une valeur existante. Un PUT sur /avancement laisserait
     * croire à une modification en place, ce que la table ne fait pas.
     *
     * 201 CREATED : une ligne est insérée.
     */
    @PostMapping("/{id}/avancement")
    public ResponseEntity<ApiResponse<AvancementResponse>> enregistrerAvancement(
            @PathVariable("id") Integer idSousActivite,
            @Valid @RequestBody AvancementRequest request) {

        return ResponseEntity
                .status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(
                        sousActiviteService.enregistrerAvancement(idSousActivite, request)));
    }
}