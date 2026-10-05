package mg.bank.backend.controller.backoffice;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.backoffice.PermissionDetailResponse;
import mg.bank.backend.dto.backoffice.PermissionFiltresResponse;
import mg.bank.backend.dto.backoffice.PermissionResponse;
import mg.bank.backend.service.PermissionService;

@RestController
@RequestMapping("/api/backoffice/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> lister(
            @RequestParam(name = "actif", required = false) Boolean actif,
            @RequestParam(name = "ressource", required = false) String ressource,
            @RequestParam(name = "action", required = false) String action) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        permissionService.lister(actif, ressource, action)));
    }

    @GetMapping("/filtres")
    public ResponseEntity<ApiResponse<PermissionFiltresResponse>> getFiltres() {
        return ResponseEntity.ok(
                ApiResponse.success(permissionService.getFiltres()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionDetailResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(permissionService.getDetail(id)));
    }
}