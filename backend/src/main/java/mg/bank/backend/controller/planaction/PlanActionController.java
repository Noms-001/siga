package mg.bank.backend.controller.planaction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import mg.bank.backend.dto.common.PageResponse;
import mg.bank.backend.dto.planaction.AvancementPlanActionDetailDTO;
import mg.bank.backend.dto.planaction.AvancementSaisieRequest;
import mg.bank.backend.dto.planaction.PlanActionDetailDTO;
import mg.bank.backend.dto.planaction.PlanActionEcritureRequest;
import mg.bank.backend.dto.planaction.PlanActionEcritureResponse;
import mg.bank.backend.dto.planaction.PlanActionListItemDTO;
import mg.bank.backend.dto.planaction.SuiviGlobalPlanActionDTO;
import mg.bank.backend.service.common.PeriodeSupport;
import mg.bank.backend.service.planaction.PlanActionService;
import mg.bank.backend.service.planaction.SuiviGlobalPlanActionService;

@RestController
@RequestMapping("/api/plans-action")
@RequiredArgsConstructor
public class PlanActionController {

        private final PlanActionService planActionService;
        private final SuiviGlobalPlanActionService suiviGlobalPlanActionService;

        @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<PlanActionListItemDTO>>> lister(
                        @RequestParam(required = false) String statut,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) Integer annee,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                        @RequestParam(required = false) Integer origineId,
                        @RequestParam(required = false) String typeOrigine,
                        @RequestParam(required = false) Integer responsableId,
                        @PageableDefault(size = 20) Pageable pagination) {

                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.rechercher(
                                                statut, search, annee, dateDebut, dateFin,
                                                origineId, typeOrigine, responsableId,
                                                pagination.getPageNumber(),
                                                pagination.getPageSize(),
                                                pagination.getSort())));
        }

        @GetMapping("/mes-brouillons")
        public ResponseEntity<ApiResponse<PageResponse<PlanActionListItemDTO>>> mesBrouillons(
                        @PageableDefault(size = 20) Pageable pagination) {
                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.mesBrouillons(
                                                pagination.getPageNumber(),
                                                pagination.getPageSize(),
                                                pagination.getSort())));
        }
        @GetMapping("/a-valider")
        public ResponseEntity<ApiResponse<PageResponse<PlanActionListItemDTO>>> aValider(
                        @PageableDefault(size = 20) Pageable pagination) {
                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.aValider(
                                                pagination.getPageNumber(),
                                                pagination.getPageSize(),
                                                pagination.getSort())));
        }

        @GetMapping("/a-demarrer")
        public ResponseEntity<ApiResponse<PageResponse<PlanActionListItemDTO>>> aDemarrer(
                        @PageableDefault(size = 20) Pageable pagination) {
                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.aDemarrer(
                                                pagination.getPageNumber(),
                                                pagination.getPageSize(),
                                                pagination.getSort())));
        }

        @PostMapping("/{id}/soumettre-validation")
        public ResponseEntity<ApiResponse<PlanActionDetailDTO>> soumettreValidation(
                        @PathVariable Integer id,
                        @RequestParam(required = false) String commentaire) {
                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.soumettre(id, commentaire)));
        }

        @PostMapping("/{id}/valider")
        public ResponseEntity<ApiResponse<PlanActionDetailDTO>> valider(
                        @PathVariable Integer id,
                        @RequestParam(defaultValue = "false") Boolean rejeter,
                        @RequestParam(required = false) String commentaire) {
                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.valider(id, Boolean.TRUE.equals(rejeter), commentaire)));
        }


        @PostMapping
        public ResponseEntity<ApiResponse<PlanActionEcritureResponse>> creer(
                        @Valid @RequestBody PlanActionEcritureRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(planActionService.creer(request)));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<PlanActionDetailDTO>> detail(
                        @PathVariable Integer id) {
                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.detail(id)));
        }

        @GetMapping("/{id}/avancements")
        public ResponseEntity<ApiResponse<List<AvancementPlanActionDetailDTO>>> listerAvancements(
                        @PathVariable Integer id) {
                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.listerAvancements(id)));
        }

        @GetMapping("/{id}/avancement-courant")
        public ResponseEntity<ApiResponse<AvancementPlanActionDetailDTO>> avancementCourant(
                        @PathVariable Integer id) {
                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.avancementCourant(id)));
        }

        @PostMapping("/{id}/avancements")
        public ResponseEntity<ApiResponse<AvancementPlanActionDetailDTO>> saisirAvancement(
                        @PathVariable Integer id,
                        @Valid @RequestBody AvancementSaisieRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(
                                                planActionService.saisirAvancement(id, request)));
        }

        @GetMapping("/suivi-global")
        public ResponseEntity<ApiResponse<SuiviGlobalPlanActionDTO>> suiviGlobal(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                        @RequestParam(required = false) Integer annee) {
                return ResponseEntity.ok(ApiResponse.success(
                                suiviGlobalPlanActionService.suiviGlobal(
                                                PeriodeSupport.resoudre(dateDebut, dateFin, annee))));
        }
}