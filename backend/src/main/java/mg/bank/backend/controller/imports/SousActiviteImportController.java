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
import mg.bank.backend.dto.importactivite.SousActiviteImportRequest;
import mg.bank.backend.service.imports.SousActiviteImportService;
import mg.bank.backend.service.imports.SousActiviteImportValidator;

@RestController
@RequestMapping("/api/activites/import/sous-activites")
@RequiredArgsConstructor
public class SousActiviteImportController {

    private final SousActiviteImportValidator validator;
    private final SousActiviteImportService service;

    @PostMapping("/valider")
    public ResponseEntity<ApiResponse<RapportValidationDTO>> valider(
            @RequestBody SousActiviteImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(validator.valider(request)));
    }

    @PostMapping("/confirmer")
    public ResponseEntity<ApiResponse<ResultatImportDTO>> confirmer(
            @RequestBody SousActiviteImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.importer(request)));
    }
}