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
import mg.bank.backend.dto.backoffice.ServiceRequest;
import mg.bank.backend.dto.backoffice.ServiceResponse;
import mg.bank.backend.service.ServiceService;

@RestController
@RequestMapping("/api/backoffice/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    /**
     * Liste des services.
     *
     * @param inclureInactifs false (défaut) → uniquement les services actifs.
     *                        true → tous les services.
     * @param idDepartement   filtre optionnel par département.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> lister(
            @RequestParam(name = "inclureInactifs", defaultValue = "true") boolean inclureInactifs,
            @RequestParam(name = "idDepartement", required = false) Integer idDepartement) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        serviceService.lister(inclureInactifs, idDepartement)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(serviceService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceResponse>> creer(
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(serviceService.creer(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(serviceService.modifier(id, request)));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<ServiceResponse>> desactiver(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(serviceService.desactiver(id)));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<ServiceResponse>> activer(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(serviceService.activer(id)));
    }
}