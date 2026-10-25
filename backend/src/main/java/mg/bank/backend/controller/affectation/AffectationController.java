package mg.bank.backend.controller.affectation;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.ActiviteListItemDTO;
import mg.bank.backend.dto.affectation.AffectationActiviteDTO;
import mg.bank.backend.dto.affectation.AffectationActiviteRequest;
import mg.bank.backend.dto.affectation.UtilisateurAffectableDTO;
import mg.bank.backend.dto.common.ActiviteFiltreCriteria;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.common.PageResponse;
import mg.bank.backend.service.affectation.AffectationService;

@RestController
@RequestMapping("/api/activites/affectations")
@RequiredArgsConstructor
public class AffectationController {

    private final AffectationService affectationService;

    /** F1 — liste des activites NON_COMMENCEE sans affectation active. */
    @GetMapping("/non-affectees")
    public ResponseEntity<ApiResponse<PageResponse<ActiviteListItemDTO>>> nonAffectees(
            @RequestParam(required = false) Boolean pta,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer prioriteId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre,
            @PageableDefault(size = 20) Pageable pagination) {

        ActiviteFiltreCriteria filtres = ActiviteFiltreCriteria.builder()
                .pta(pta)
                .search(search)
                .serviceId(serviceId)
                .prioriteId(prioriteId)
                .annee(annee)
                .trimestre(trimestre)
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                affectationService.rechercherNonAffectees(
                        filtres,
                        pagination.getPageNumber(),
                        pagination.getPageSize(),
                        pagination.getSort())));
    }

    /** F2 — historique des affectations d'une activite. */
    @GetMapping("/{idActivite}")
    public ResponseEntity<ApiResponse<List<AffectationActiviteDTO>>> historique(
            @PathVariable Integer idActivite) {
        return ResponseEntity.ok(ApiResponse.success(
                affectationService.listerAffectations(idActivite)));
    }

    /** F3 — historique des affectations d'une sous-activite. */
    @GetMapping("/{idActivite}/sous-activites/{idSousActivite}")
    public ResponseEntity<ApiResponse<List<AffectationActiviteDTO>>> historiqueSousActivite(
            @PathVariable Integer idActivite,
            @PathVariable Integer idSousActivite) {
        return ResponseEntity.ok(ApiResponse.success(
                affectationService.listerAffectationsSousActivite(idActivite, idSousActivite)));
    }

    /** F4 — utilisateurs du service de l'activite, actifs. */
    @GetMapping("/{idActivite}/utilisateurs-disponibles")
    public ResponseEntity<ApiResponse<List<UtilisateurAffectableDTO>>> utilisateursDisponibles(
            @PathVariable Integer idActivite) {
        return ResponseEntity.ok(ApiResponse.success(
                affectationService.utilisateursAffectables(idActivite)));
    }

    /** F5 — affecter une activite. */
    @PostMapping("/{idActivite}")
    public ResponseEntity<ApiResponse<AffectationActiviteDTO>> affecterActivite(
            @PathVariable Integer idActivite,
            @Valid @RequestBody AffectationActiviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        affectationService.affecterActivite(idActivite, request)));
    }

    /** F6 — affecter une sous-activite. */
    @PostMapping("/{idActivite}/sous-activites/{idSousActivite}")
    public ResponseEntity<ApiResponse<AffectationActiviteDTO>> affecterSousActivite(
            @PathVariable Integer idActivite,
            @PathVariable Integer idSousActivite,
            @Valid @RequestBody AffectationActiviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        affectationService.affecterSousActivite(
                                idActivite, idSousActivite, request)));
    }

    /** F7 — desaffecter une activite. */
    @DeleteMapping("/{idActivite}/{idAffectation}")
    public ResponseEntity<Void> desaffecterActivite(
            @PathVariable Integer idActivite,
            @PathVariable Integer idAffectation) {
        affectationService.desaffecterActivite(idActivite, idAffectation);
        return ResponseEntity.noContent().build();
    }

    /** F8 — desaffecter une sous-activite. */
    @DeleteMapping("/{idActivite}/sous-activites/{idSousActivite}/{idAffectation}")
    public ResponseEntity<Void> desaffecterSousActivite(
            @PathVariable Integer idActivite,
            @PathVariable Integer idSousActivite,
            @PathVariable Integer idAffectation) {
        affectationService.desaffecterSousActivite(idActivite, idSousActivite, idAffectation);
        return ResponseEntity.noContent().build();
    }
}