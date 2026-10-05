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
import mg.bank.backend.dto.backoffice.SiteRequest;
import mg.bank.backend.dto.backoffice.SiteResponse;
import mg.bank.backend.service.SiteService;

@RestController
@RequestMapping("/api/backoffice/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteResponse>>> lister(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.lister(inclureInactifs)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SiteResponse>> creer(
            @Valid @RequestBody SiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(siteService.creer(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody SiteRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.modifier(id, request)));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<SiteResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.desactiver(id)));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<SiteResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(siteService.activer(id)));
    }
}