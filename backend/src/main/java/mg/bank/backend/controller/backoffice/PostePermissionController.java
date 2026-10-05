package mg.bank.backend.controller.backoffice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.backoffice.PermissionPourPosteResponse;
import mg.bank.backend.dto.backoffice.PostePermissionRequest;
import mg.bank.backend.dto.backoffice.PostePermissionUpdateRequest;
import mg.bank.backend.service.PostePermissionService;

@RestController
@RequestMapping("/api/backoffice/postes/{idPoste}/permissions")
@RequiredArgsConstructor
public class PostePermissionController {

    private final PostePermissionService postePermissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionPourPosteResponse>>> lister(
            @PathVariable Integer idPoste) {
        return ResponseEntity.ok(
                ApiResponse.success(postePermissionService.lister(idPoste)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PermissionPourPosteResponse>> affecter(
            @PathVariable Integer idPoste,
            @Valid @RequestBody PostePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        postePermissionService.affecter(idPoste, request)));
    }

    @PutMapping("/{idPermission}")
    public ResponseEntity<ApiResponse<PermissionPourPosteResponse>> modifierPortee(
            @PathVariable Integer idPoste,
            @PathVariable Integer idPermission,
            @Valid @RequestBody PostePermissionUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        postePermissionService.modifierPortee(idPoste, idPermission, request)));
    }

    @DeleteMapping("/{idPermission}")
    public ResponseEntity<ApiResponse<Void>> retirer(
            @PathVariable Integer idPoste,
            @PathVariable Integer idPermission) {
        postePermissionService.retirer(idPoste, idPermission);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}