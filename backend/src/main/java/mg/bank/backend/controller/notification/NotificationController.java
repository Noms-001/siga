package mg.bank.backend.controller.notification;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.notification.NotificationDTO;
import mg.bank.backend.dto.notification.NotificationDetailDTO;
import mg.bank.backend.model.parametre.Parametre;
import mg.bank.backend.repository.parametre.ParametreRepository;
import mg.bank.backend.service.notification.NotificationService;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

        private static final String CODE_PRIORITES = "NOTIFICATION_EMAIL_PRIORITES";
        private static final String DEFAULT_PRIORITES = "CRITIQUE,HAUTE";

        private final NotificationService notificationService;
        private final CurrentUserService currentUserService;
        private final ParametreRepository parametreRepository;

        // ------------------------------------------------------------------
        // Liste paginée
        // ------------------------------------------------------------------

        @GetMapping
        public ResponseEntity<ApiResponse<List<NotificationDTO>>> lister() {
                Integer idUtilisateur = currentUserService.getId();
                return ResponseEntity.ok(ApiResponse.success(
                                notificationService.lister(idUtilisateur)));
        }

        @GetMapping("/page")
        public ResponseEntity<ApiResponse<Page<NotificationDTO>>> page(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "TOUTES") String lecture,
                        @RequestParam(required = false) String priorite,
                        @RequestParam(defaultValue = "false") boolean urgentes) {

                Integer idUtilisateur = currentUserService.getId();

                List<String> prioritesFiltre = urgentes
                                ? lirePrioritesUrgentes()
                                : (priorite == null || priorite.isBlank()
                                                ? null
                                                : List.of(priorite));

                Pageable pageable = PageRequest.of(page, size);
                return ResponseEntity.ok(ApiResponse.success(
                                notificationService.listerPage(
                                                idUtilisateur, search, lecture, prioritesFiltre, pageable)));
        }

        // ------------------------------------------------------------------
        // Détail enrichi
        // ------------------------------------------------------------------

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<NotificationDetailDTO>> detail(@PathVariable Integer id) {
                Integer idUtilisateur = currentUserService.getId();
                return ResponseEntity.ok(ApiResponse.success(
                                notificationService.detail(id, idUtilisateur)));
        }

        // ------------------------------------------------------------------
        // Compteur non lues
        // ------------------------------------------------------------------

        @GetMapping("/non-lues/count")
        public ResponseEntity<ApiResponse<Long>> nonLues() {
                Integer idUtilisateur = currentUserService.getId();
                return ResponseEntity.ok(ApiResponse.success(
                                notificationService.compterNonLues(idUtilisateur)));
        }

        // ------------------------------------------------------------------
        // Marquer lue(s)
        // ------------------------------------------------------------------

        @PutMapping("/{id}/lue")
        public ResponseEntity<ApiResponse<Boolean>> marquerLue(@PathVariable Integer id) {
                Integer idUtilisateur = currentUserService.getId();
                return ResponseEntity.ok(ApiResponse.success(
                                notificationService.marquerLue(idUtilisateur, id)));
        }

        @PutMapping("/lues")
        public ResponseEntity<ApiResponse<Integer>> marquerToutesLues() {
                Integer idUtilisateur = currentUserService.getId();
                return ResponseEntity.ok(ApiResponse.success(
                                notificationService.marquerToutesLues(idUtilisateur)));
        }

        // ------------------------------------------------------------------
        // Helper — priorités urgentes
        // ------------------------------------------------------------------

        /**
         * Lit le paramètre {@code NOTIFICATION_EMAIL_PRIORITES}.
         *
         * <p>
         * Retourne {@code ["CRITIQUE", "HAUTE"]} si le paramètre est
         * absent, vide ou mal formé — pour que la route /page?urgentes=true
         * reste fonctionnelle même si la configuration backoffice a été
         * modifiée par erreur.
         * </p>
         */
        private List<String> lirePrioritesUrgentes() {
                return parametreRepository.findByCodeAndActifTrue(CODE_PRIORITES)
                                .map(Parametre::getValeur)
                                .filter(v -> v != null && !v.isBlank())
                                .map(v -> Arrays.stream(v.split(","))
                                                .map(String::trim)
                                                .filter(s -> !s.isEmpty())
                                                .toList())
                                .filter(l -> !l.isEmpty())
                                .orElseGet(() -> List.of(DEFAULT_PRIORITES.split(",")));
        }
}