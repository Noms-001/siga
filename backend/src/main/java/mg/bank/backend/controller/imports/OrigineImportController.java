package mg.bank.backend.controller.imports;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.importactivite.OrigineImportRequest;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.service.imports.OrigineImportService;
import mg.bank.backend.service.imports.OrigineImportValidator;

@RestController
@RequestMapping("/api/activites/import/origines")
@RequiredArgsConstructor
public class OrigineImportController {

    private final OrigineImportValidator validator;
    private final OrigineImportService service;

    @PostMapping("/valider")
    public ResponseEntity<ApiResponse<RapportValidationDTO>> valider(
            @RequestBody OrigineImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(validator.valider(request)));
    }

    @PostMapping("/confirmer")
    public ResponseEntity<ApiResponse<ResultatImportDTO>> confirmer(
            @RequestBody OrigineImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.importer(request)));
    }
}