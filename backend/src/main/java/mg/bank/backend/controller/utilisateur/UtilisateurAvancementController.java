package mg.bank.backend.controller.utilisateur;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.utilisateur.UtilisateurAvancementDTO;
import mg.bank.backend.service.utilisateur.UtilisateurAvancementService;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurAvancementController {

    private final UtilisateurAvancementService utilisateurAvancementService;

    /**
     * Avancement d'un utilisateur sur une annee : activites et plans
     * d'action auxquels il est affecte, avec leur avancement courant.
     *
     * L'annee est facultative : par defaut, l'annee en cours.
     */
    @GetMapping("/{id}/avancement-annee")
    public ResponseEntity<ApiResponse<UtilisateurAvancementDTO>> avancementAnnee(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(ApiResponse.success(
                utilisateurAvancementService.avancementAnnee(id, annee)));
    }
}