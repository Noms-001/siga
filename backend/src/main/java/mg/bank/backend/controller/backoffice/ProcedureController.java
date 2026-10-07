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
import mg.bank.backend.dto.backoffice.ProcedureRequest;
import mg.bank.backend.dto.backoffice.ProcedureResponse;
import mg.bank.backend.service.ProcedureService;

@RestController
@RequestMapping("/api/backoffice/procedures")
@RequiredArgsConstructor
public class ProcedureController {

        private final ProcedureService procedureService;

        @GetMapping
        public ResponseEntity<ApiResponse<List<ProcedureResponse>>> lister(
                        @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
                return ResponseEntity.ok(
                                ApiResponse.success(procedureService.lister(inclureInactifs)));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<ProcedureResponse>> getById(@PathVariable Integer id) {
                return ResponseEntity.ok(
                                ApiResponse.success(procedureService.getById(id)));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<ProcedureResponse>> creer(
                        @Valid @RequestBody ProcedureRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(procedureService.creer(request)));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<ProcedureResponse>> modifier(
                        @PathVariable Integer id,
                        @Valid @RequestBody ProcedureRequest request) {
                return ResponseEntity.ok(
                                ApiResponse.success(procedureService.modifier(id, request)));
        }

        @PatchMapping("/{id}/desactiver")
        public ResponseEntity<ApiResponse<ProcedureResponse>> desactiver(@PathVariable Integer id) {
                return ResponseEntity.ok(
                                ApiResponse.success(procedureService.desactiver(id)));
        }

        @PatchMapping("/{id}/activer")
        public ResponseEntity<ApiResponse<ProcedureResponse>> activer(@PathVariable Integer id) {
                return ResponseEntity.ok(
                                ApiResponse.success(procedureService.activer(id)));
        }
}