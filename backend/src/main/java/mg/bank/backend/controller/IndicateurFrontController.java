package mg.bank.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.backoffice.IndicateurResponse;
import mg.bank.backend.service.IndicateurService;

/**
 * Consultation des indicateurs côté Frontoffice.
 *
 * Lecture seule : le frontoffice n'a besoin ni de créer, ni de modifier, ni
 * de désactiver un indicateur. Toute écriture passe par le CRUD backoffice.
 *
 * Réutilise IndicateurService tel quel — aucune duplication de logique.
 * Ce controller n'existe que pour exposer les mêmes données sous un chemin
 * frontoffice (/api/indicateurs), cohérent avec la séparation du projet
 * entre /api/xxx (frontoffice) et /api/backoffice/xxx (backoffice).
 *
 * Le filtre `actif` par défaut est TRUE : un utilisateur frontoffice ne
 * doit pas voir les indicateurs désactivés. Le backoffice peut les voir en
 * passant actif=false ou actif absent.
 */
@RestController
@RequestMapping("/api/indicateurs")
@RequiredArgsConstructor
public class IndicateurFrontController {

    private final IndicateurService indicateurService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IndicateurResponse>>> lister(
            @RequestParam(name = "actif", defaultValue = "true") Boolean actif,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(
                ApiResponse.success(indicateurService.lister(actif, type, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IndicateurResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(indicateurService.getById(id)));
    }
}