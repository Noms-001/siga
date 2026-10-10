package mg.bank.backend.controller;

import java.time.LocalDate;
import java.util.List;

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
import mg.bank.backend.dto.backoffice.CodeProposeResponse;
import mg.bank.backend.dto.backoffice.OrigineFiltresResponse;
import mg.bank.backend.dto.backoffice.OrigineRequest;
import mg.bank.backend.dto.backoffice.OrigineResponse;
import mg.bank.backend.dto.backoffice.SignalementDetailResponse;
import mg.bank.backend.service.OrigineService;

/**
 * Signalements côté Frontoffice — incidents, risques, et tout autre type
 * présent dans `origine`.
 *
 * Lecture + création. Pas de modification ni de suppression : une origine
 * est un fait historique. Le chemin est métier (/api/signalements), la
 * table sous-jacente reste `origine` — décision documentée pour éviter
 * la surprise quand on inspecte la base.
 */
@RestController
@RequestMapping("/api/signalements")
@RequiredArgsConstructor
public class SignalementFrontController {

    private final OrigineService origineService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrigineResponse>>> lister(
            @RequestParam(name = "types", required = false) String types,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "avecPlan", required = false) Boolean avecPlan,
            @RequestParam(name = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(
                ApiResponse.success(origineService.lister(types, search, avecPlan, annee)));
    }

    @GetMapping("/filtres")
    public ResponseEntity<ApiResponse<OrigineFiltresResponse>> getFiltres() {
        return ResponseEntity.ok(
                ApiResponse.success(origineService.getFiltres()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrigineResponse>> creer(
            @Valid @RequestBody OrigineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(origineService.creer(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SignalementDetailResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(origineService.getDetail(id)));
    }

    /**
     * Code proposé pour un nouveau signalement du type demandé.
     *
     * Déclaré AVANT `/{id}` pour ne pas être capturé comme un identifiant.
     * Le chemin est volontairement en GET : la réponse est une valeur
     * calculée, pas une ressource. Un POST serait plus régulier et
     * obligerait à envoyer un corps pour obtenir un aperçu.
     *
     * L'année n'est pas passée en paramètre : elle est toujours celle de la
     * création, côté serveur. Un client qui fournirait une autre année
     * obtiendrait un aperçu qui ne correspondrait pas au code réellement
     * attribué.
     */
    @GetMapping("/prochain-code")
    public ResponseEntity<ApiResponse<CodeProposeResponse>> prochainCode(
            @RequestParam("type") String type) {
        String code = origineService.prochainCode(type);
        return ResponseEntity.ok(
                ApiResponse.success(CodeProposeResponse.builder()
                        .code(code)
                        .typeOrigine(type)
                        .annee(LocalDate.now().getYear())
                        .build()));
    }
}