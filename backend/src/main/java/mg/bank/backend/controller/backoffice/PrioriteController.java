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
import mg.bank.backend.dto.backoffice.PrioriteResponse;
import mg.bank.backend.service.PrioriteService;

@RestController
@RequestMapping("/api/backoffice/priorites")
@RequiredArgsConstructor
public class PrioriteController {

    private final PrioriteService prioriteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PrioriteResponse>>> lister(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return ResponseEntity.ok(
                ApiResponse.success(prioriteService.lister(inclureInactifs)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrioriteResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(prioriteService.getById(id)));
    }
}