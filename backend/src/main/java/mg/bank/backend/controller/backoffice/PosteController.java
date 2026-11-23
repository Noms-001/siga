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
import mg.bank.backend.dto.backoffice.PosteRequest;
import mg.bank.backend.dto.backoffice.PosteResponse;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.service.utilisateur.PosteService;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/backoffice/postes")
@RequiredArgsConstructor
public class PosteController {

    private final PosteService posteService;

    @RequirePermission(ressource = PermissionResource.POSTE, action = PermissionAction.CONSULTER)
    @GetMapping
    public ResponseEntity<ApiResponse<List<PosteResponse>>> lister(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.lister(inclureInactifs)));
    }

    @RequirePermission(ressource = PermissionResource.POSTE, action = PermissionAction.CONSULTER)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PosteResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.getById(id)));
    }

    @RequirePermission(ressource = PermissionResource.POSTE, action = PermissionAction.CREER)
    @PostMapping
    public ResponseEntity<ApiResponse<PosteResponse>> creer(
            @Valid @RequestBody PosteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(posteService.creer(request)));
    }

    @RequirePermission(ressource = PermissionResource.POSTE, action = PermissionAction.MODIFIER)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PosteResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody PosteRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.modifier(id, request)));
    }

    @RequirePermission(ressource = PermissionResource.POSTE, action = PermissionAction.DESACTIVER)
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<PosteResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.desactiver(id)));
    }

    @RequirePermission(ressource = PermissionResource.POSTE, action = PermissionAction.ACTIVER)
    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<PosteResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(posteService.activer(id)));
    }
}
