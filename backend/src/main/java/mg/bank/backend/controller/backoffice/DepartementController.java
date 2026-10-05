package mg.bank.backend.controller.backoffice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import mg.bank.backend.dto.backoffice.DepartementRequest;
import mg.bank.backend.dto.backoffice.DepartementResponse;
import mg.bank.backend.service.DepartementService;

@RestController
@RequestMapping("/api/backoffice/departements")
@RequiredArgsConstructor
public class DepartementController {

    private final DepartementService departementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartementResponse>>> lister() {
        return ResponseEntity.ok(
                ApiResponse.success(departementService.lister()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartementResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(departementService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartementResponse>> creer(
            @Valid @RequestBody DepartementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(departementService.creer(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartementResponse>> modifier(
            @PathVariable Integer id,
            @Valid @RequestBody DepartementRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(departementService.modifier(id, request)));
    }
}