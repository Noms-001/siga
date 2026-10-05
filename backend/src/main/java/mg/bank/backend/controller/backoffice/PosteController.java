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
import mg.bank.backend.dto.backoffice.PosteRequest;
import mg.bank.backend.dto.backoffice.PosteResponse;
import mg.bank.backend.service.PosteService;

@RestController
@RequestMapping("/api/backoffice/postes")
@RequiredArgsConstructor
public class PosteController {

    private final PosteService posteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PosteResponse>>> lister(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.lister(inclureInactifs)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PosteResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PosteResponse>> creer(
            @Valid @RequestBody PosteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(posteService.creer(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PosteResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody PosteRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.modifier(id, request)));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<PosteResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.desactiver(id)));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<PosteResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.activer(id)));
    }
}