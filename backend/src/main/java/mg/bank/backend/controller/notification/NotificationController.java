package mg.bank.backend.controller.notification;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.common.PageResponse;
import mg.bank.backend.dto.notification.NotificationDTO;
import mg.bank.backend.service.notification.NotificationService;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> lister() {
        return ResponseEntity.ok(ApiResponse
                .success(notificationService.lister(userIdCourant())));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDTO>>> listerPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "TOUTES") String lecture,
            @RequestParam(required = false) String priorite) {
        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        String recherche = search == null || search.isBlank() ? null : search.trim();
        String filtreLecture = switch (lecture.toUpperCase()) {
            case "NON_LUES", "LUES" -> lecture.toUpperCase();
            default -> "TOUTES";
        };
        String filtrePriorite = priorite == null || priorite.isBlank() ? null : priorite.trim().toUpperCase();
        var resultat = notificationService.listerPage(
                userIdCourant(),
                recherche,
                filtreLecture,
                filtrePriorite,
                PageRequest.of(pageIndex, pageSize));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(resultat)));
    }

    @GetMapping("/non-lues/count")
    public ResponseEntity<ApiResponse<Long>> compter() {
        return ResponseEntity.ok(ApiResponse
                .success(notificationService.compterNonLues(userIdCourant())));
    }

    @PutMapping("/{id}/lue")
    public ResponseEntity<ApiResponse<Boolean>> marquerLue(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse
                .success(notificationService.marquerLue(userIdCourant(), id)));
    }

    @PutMapping("/lues")
    public ResponseEntity<ApiResponse<Integer>> marquerToutesLues() {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.marquerToutesLues(userIdCourant())));
    }

    private Integer userIdCourant() {
        return currentUserService.get().getIdUtilisateur();
    }
}
