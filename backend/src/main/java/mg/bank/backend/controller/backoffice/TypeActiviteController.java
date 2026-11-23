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
import mg.bank.backend.dto.backoffice.TypeActiviteRequest;
import mg.bank.backend.dto.backoffice.TypeActiviteResponse;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.service.referentiel.TypeActiviteService;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/backoffice/types-activite")
@RequiredArgsConstructor
public class TypeActiviteController {

    private final TypeActiviteService typeActiviteService;

    @RequirePermission(ressource = PermissionResource.TYPE_ACTIVITE, action = PermissionAction.CONSULTER)
    @GetMapping
    public ResponseEntity<ApiResponse<List<TypeActiviteResponse>>> lister(
            @RequestParam(name = "actif", required = false) Boolean actif,
            @RequestParam(name = "idService", required = false) Integer idService) {
        return ResponseEntity.ok(
                ApiResponse.success(typeActiviteService.lister(actif, idService)));
    }

    @RequirePermission(ressource = PermissionResource.TYPE_ACTIVITE, action = PermissionAction.CONSULTER)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TypeActiviteResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(typeActiviteService.getById(id)));
    }

    @RequirePermission(ressource = PermissionResource.TYPE_ACTIVITE, action = PermissionAction.CREER)
    @PostMapping
    public ResponseEntity<ApiResponse<TypeActiviteResponse>> creer(
            @Valid @RequestBody TypeActiviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(typeActiviteService.creer(request)));
    }

    @RequirePermission(ressource = PermissionResource.TYPE_ACTIVITE, action = PermissionAction.MODIFIER)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TypeActiviteResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody TypeActiviteRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(typeActiviteService.modifier(id, request)));
    }

    @RequirePermission(ressource = PermissionResource.TYPE_ACTIVITE, action = PermissionAction.DESACTIVER)
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<TypeActiviteResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(typeActiviteService.desactiver(id)));
    }

    @RequirePermission(ressource = PermissionResource.TYPE_ACTIVITE, action = PermissionAction.ACTIVER)
    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<TypeActiviteResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(typeActiviteService.activer(id)));
    }
}
