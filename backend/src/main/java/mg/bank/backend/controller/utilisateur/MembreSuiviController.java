package mg.bank.backend.controller.utilisateur;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.utilisateur.MembreSuiviGlobalDTO;
import mg.bank.backend.dto.utilisateur.MembreSuiviMembresDTO;
import mg.bank.backend.service.utilisateur.MembreSuiviService;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/utilisateurs/suivi")
@RequiredArgsConstructor
public class MembreSuiviController {

    private final MembreSuiviService membreSuiviService;

    @RequirePermission(ressource = PermissionResource.MEMBRE_SUIVI, action = PermissionAction.CONSULTER)
    @GetMapping("/membres")
    public ResponseEntity<ApiResponse<MembreSuiviMembresDTO>> membres(
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer idService,
            @RequestParam(required = false) String recherche) {
        return ResponseEntity.ok(ApiResponse.success(membreSuiviService.membres(
                dateDebut, dateFin, annee, trimestre, idService, recherche)));
    }

    @RequirePermission(ressource = PermissionResource.MEMBRE_SUIVI, action = PermissionAction.CONSULTER)
    @GetMapping("/global")
    public ResponseEntity<ApiResponse<MembreSuiviGlobalDTO>> global(
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer idService,
            @RequestParam(required = false) String categorie) {
        return ResponseEntity.ok(ApiResponse.success(membreSuiviService.global(
                dateDebut, dateFin, annee, trimestre, idService, categorie)));
    }

    @RequirePermission(ressource = PermissionResource.MEMBRE_SUIVI, action = PermissionAction.CONSULTER)
    @GetMapping("/elements")
    public ResponseEntity<ApiResponse<List<MembreSuiviGlobalDTO.ElementDTO>>> elements(
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer idService,
            @RequestParam String categorie) {
        return ResponseEntity.ok(ApiResponse.success(membreSuiviService.elements(
                dateDebut, dateFin, annee, trimestre, idService, categorie)));
    }
}
