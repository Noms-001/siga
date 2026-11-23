package mg.bank.backend.controller.imports;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.dto.importactivite.ResultatIntermediaireImportRequest;
import mg.bank.backend.service.imports.ResultatIntermediaireImportService;
import mg.bank.backend.service.imports.ResultatIntermediaireImportValidator;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/activites/import/resultats")
@RequiredArgsConstructor
public class ResultatIntermediaireImportController {

    private final ResultatIntermediaireImportValidator validator;
    private final ResultatIntermediaireImportService service;

    @RequirePermission(ressource = PermissionResource.RESULTAT_INTERMEDIAIRE, action = PermissionAction.IMPORTER)
    @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.IMPORTER)
    @PostMapping("/valider")
    public ResponseEntity<ApiResponse<RapportValidationDTO>> valider(
            @RequestBody ResultatIntermediaireImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(validator.valider(request)));
    }

    @RequirePermission(ressource = PermissionResource.RESULTAT_INTERMEDIAIRE, action = PermissionAction.IMPORTER)
    @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.IMPORTER)
    @PostMapping("/confirmer")
    public ResponseEntity<ApiResponse<ResultatImportDTO>> confirmer(
            @RequestBody ResultatIntermediaireImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.importer(request)));
    }
}
