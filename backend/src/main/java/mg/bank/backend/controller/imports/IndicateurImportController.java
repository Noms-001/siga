package mg.bank.backend.controller.imports;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.importactivite.IndicateurImportRequest;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.service.imports.IndicateurImportService;
import mg.bank.backend.service.imports.IndicateurImportValidator;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/activites/import/indicateurs")
@RequiredArgsConstructor
public class IndicateurImportController {

    private final IndicateurImportValidator validator;
    private final IndicateurImportService service;

    @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.IMPORTER)
    @PostMapping("/valider")
    public ResponseEntity<ApiResponse<RapportValidationDTO>> valider(
            @RequestBody IndicateurImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(validator.valider(request)));
    }

    @RequirePermission(ressource = PermissionResource.INDICATEUR, action = PermissionAction.IMPORTER)
    @PostMapping("/confirmer")
    public ResponseEntity<ApiResponse<ResultatImportDTO>> confirmer(
            @RequestBody IndicateurImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.importer(request)));
    }
}
