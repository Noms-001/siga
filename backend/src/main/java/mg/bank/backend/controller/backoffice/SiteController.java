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
import mg.bank.backend.dto.backoffice.SiteRequest;
import mg.bank.backend.dto.backoffice.SiteResponse;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.service.referentiel.SiteService;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/backoffice/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @RequirePermission(ressource = PermissionResource.SITE, action = PermissionAction.CONSULTER)
    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteResponse>>> lister(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.lister(inclureInactifs)));
    }

    @RequirePermission(ressource = PermissionResource.SITE, action = PermissionAction.CONSULTER)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.getById(id)));
    }

    @RequirePermission(ressource = PermissionResource.SITE, action = PermissionAction.CREER)
    @PostMapping
    public ResponseEntity<ApiResponse<SiteResponse>> creer(
            @Valid @RequestBody SiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(siteService.creer(request)));
    }

    @RequirePermission(ressource = PermissionResource.SITE, action = PermissionAction.MODIFIER)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody SiteRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.modifier(id, request)));
    }

    @RequirePermission(ressource = PermissionResource.SITE, action = PermissionAction.DESACTIVER)
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<SiteResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.desactiver(id)));
    }

    @RequirePermission(ressource = PermissionResource.SITE, action = PermissionAction.ACTIVER)
    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<SiteResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.activer(id)));
    }
}
