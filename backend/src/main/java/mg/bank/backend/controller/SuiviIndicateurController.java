package mg.bank.backend.controller;

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
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.indicateur.AgregationIndicateurDTO;
import mg.bank.backend.dto.indicateur.AtteinteIndicateurDTO;
import mg.bank.backend.dto.indicateur.IndicateurEnRetardDTO;
import mg.bank.backend.dto.indicateur.SuiviIndicateurDTO;
import mg.bank.backend.dto.indicateur.ValeurIndicateurDTO;
import mg.bank.backend.dto.indicateur.ValeurIndicateurRequest;
import mg.bank.backend.enums.MethodeAgregation;
import mg.bank.backend.service.SuiviIndicateurService;

@RestController
@RequestMapping("/api/indicateurs")
@RequiredArgsConstructor
public class SuiviIndicateurController {

        private final SuiviIndicateurService suiviIndicateurService;

        @GetMapping("/{id}/valeurs")
        public ResponseEntity<ApiResponse<Page<ValeurIndicateurDTO>>> historique(
                        @PathVariable Integer id,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                return ResponseEntity.ok(
                                ApiResponse.success(suiviIndicateurService.historique(id, page, size)));
        }

        @PostMapping("/{id}/valeurs")
        public ResponseEntity<ApiResponse<ValeurIndicateurDTO>> saisir(
                        @PathVariable Integer id,
                        @Valid @RequestBody ValeurIndicateurRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(suiviIndicateurService.saisir(id, request)));
        }

        @GetMapping("/{id}/valeur-courante")
        public ResponseEntity<ApiResponse<ValeurIndicateurDTO>> valeurCourante(
                        @PathVariable Integer id) {
                return ResponseEntity.ok(
                                ApiResponse.success(suiviIndicateurService.valeurCourante(id)));
        }

        @GetMapping("/{id}/atteinte")
        public ResponseEntity<ApiResponse<AtteinteIndicateurDTO>> atteinte(
                        @PathVariable Integer id) {
                return ResponseEntity.ok(
                                ApiResponse.success(suiviIndicateurService.atteinte(id)));
        }

        @GetMapping("/activites/{idActivite}/suivi")
        public ResponseEntity<ApiResponse<SuiviIndicateurDTO>> suiviActivite(
                        @PathVariable Integer idActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(suiviIndicateurService.suiviActivite(idActivite)));
        }

        @GetMapping("/{id}/agregation")
        public ResponseEntity<ApiResponse<AgregationIndicateurDTO>> agregation(
                        @PathVariable Integer id,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
                        @RequestParam MethodeAgregation methode) {
                return ResponseEntity.ok(ApiResponse.success(
                                suiviIndicateurService.agreger(id, debut, fin, methode)));
        }

        @GetMapping("/en-retard")
        public ResponseEntity<ApiResponse<List<IndicateurEnRetardDTO>>> enRetard(
                        @RequestParam(defaultValue = "false") boolean inclureAJour,
                        @RequestParam(defaultValue = "true") boolean inclureJamaisSaisi) {
                return ResponseEntity.ok(ApiResponse.success(
                                suiviIndicateurService.listerEnRetard(inclureAJour, inclureJamaisSaisi)));
        }
}