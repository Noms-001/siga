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
import mg.bank.backend.dto.backoffice.StatutResponse;
import mg.bank.backend.service.StatutService;

@RestController
@RequestMapping("/api/backoffice/statuts")
@RequiredArgsConstructor
public class StatutController {

    private final StatutService statutService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StatutResponse>>> lister(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return ResponseEntity.ok(
                ApiResponse.success(statutService.lister(inclureInactifs)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StatutResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(statutService.getById(id)));
    }
}