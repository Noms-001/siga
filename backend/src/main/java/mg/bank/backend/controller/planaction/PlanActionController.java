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
import mg.bank.backend.service.planaction.PlanActionService;

@RestController
@RequestMapping("/api/plans-action")
@RequiredArgsConstructor
public class PlanActionController {

        private final PlanActionService planActionService;

        @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<PlanActionListItemDTO>>> lister(
                        @RequestParam(required = false) String statut,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) Integer annee,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                        @RequestParam(required = false) Integer origineId,
                        @RequestParam(required = false) Integer responsableId,
                        @PageableDefault(size = 20) Pageable pagination) {

                return ResponseEntity.ok(ApiResponse.success(
                                planActionService.rechercher(
                                                statut, search, annee, dateDebut, dateFin,
                                                origineId, responsableId,
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
}