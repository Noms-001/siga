package mg.bank.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.importactivite.ActiviteImportRequest;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.service.ActiviteImportService;
import mg.bank.backend.service.ActiviteImportValidator;

@RestController
@RequestMapping("/api/activites/import")
@RequiredArgsConstructor
public class ActiviteImportController {

    private final ActiviteImportValidator activiteImportValidator;
    private final ActiviteImportService activiteImportService;

    @PostMapping("/valider")
    public ResponseEntity<ApiResponse<RapportValidationDTO>> valider(
            @RequestBody ActiviteImportRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(activiteImportValidator.valider(request)));
    }

    @PostMapping("/confirmer")
    public ResponseEntity<ApiResponse<ResultatImportDTO>> confirmer(
            @RequestBody ActiviteImportRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(activiteImportService.importer(request)));
    }
}