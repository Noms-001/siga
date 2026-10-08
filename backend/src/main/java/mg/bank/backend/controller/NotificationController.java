package mg.bank.backend.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.NotificationDTO;
import mg.bank.backend.service.ActiviteService;
import mg.bank.backend.service.NotificationService;

@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final ActiviteService activiteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> lister() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.lister(activiteService.utilisateurCourant().getIdUtilisateur())));
    }
    @GetMapping("/non-lues/count")
    public ResponseEntity<ApiResponse<Long>> compter() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.compterNonLues(activiteService.utilisateurCourant().getIdUtilisateur())));
    }
    @PutMapping("/{id}/lue")
    public ResponseEntity<ApiResponse<Boolean>> marquerLue(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.marquerLue(activiteService.utilisateurCourant().getIdUtilisateur(), id)));
    }
}
