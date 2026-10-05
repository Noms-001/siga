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
import mg.bank.backend.dto.backoffice.RoleRequest;
import mg.bank.backend.dto.backoffice.RoleResponse;
import mg.bank.backend.service.RoleService;

@RestController
@RequestMapping("/api/backoffice/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> lister(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return ResponseEntity.ok(
                ApiResponse.success(roleService.lister(inclureInactifs)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(roleService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> creer(
            @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(roleService.creer(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(roleService.modifier(id, request)));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<RoleResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(roleService.desactiver(id)));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<RoleResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(roleService.activer(id)));
    }
}