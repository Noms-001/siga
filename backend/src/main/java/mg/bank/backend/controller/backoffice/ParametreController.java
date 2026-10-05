package mg.bank.backend.controller.backoffice;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.backoffice.ParametreFiltresResponse;
import mg.bank.backend.dto.backoffice.ParametreResponse;
import mg.bank.backend.dto.backoffice.ParametreUpdateRequest;
import mg.bank.backend.service.ParametreService;

@RestController
@RequestMapping("/api/backoffice/parametres")
@RequiredArgsConstructor
public class ParametreController {

    private final ParametreService parametreService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParametreResponse>>> lister(
            @RequestParam(name = "actif", required = false) Boolean actif,
            @RequestParam(name = "categorie", required = false) String categorie) {
        return ResponseEntity.ok(
                ApiResponse.success(parametreService.lister(actif, categorie)));
    }

    @GetMapping("/filtres")
    public ResponseEntity<ApiResponse<ParametreFiltresResponse>> getFiltres() {
        return ResponseEntity.ok(
                ApiResponse.success(parametreService.getFiltres()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ParametreResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(parametreService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ParametreResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody ParametreUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(parametreService.modifier(id, request)));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<ParametreResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(parametreService.desactiver(id)));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<ParametreResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(parametreService.activer(id)));
    }
}