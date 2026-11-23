package mg.bank.backend.service.imports;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.importactivite.ErreurImportDTO;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.service.activite.ActiviteService;
import mg.bank.backend.service.common.PorteeService;

/**
 * Résolution et autorisation d'une activité cible par sa référence.
 * Partagé entre les 4 nouveaux imports (Sous-Activités, Résultats,
 * Indicateurs, Origines).
 */
@Service
@RequiredArgsConstructor
public class ImportActiviteLookup {

    private final ActiviteRepository activiteRepository;
    private final ActiviteService activiteService;
    private final PorteeService porteeService;

    /** Activité ou null si la référence est vide ou absente de la base. */
    public Activite parReference(String reference) {
        if (reference == null || reference.isBlank()) return null;
        return activiteRepository.findByReferenceIgnoreCase(reference.trim()).orElse(null);
    }

    /**
     * Résout l'activité et vérifie le périmètre. Ajoute une erreur si
     * la référence est manquante, absente de la base, ou inaccessible.
     * Retourne null dans tous ces cas.
     */
    public Activite parReferenceAvecErreurs(String reference,
                                            String idActivite,
                                            List<ErreurImportDTO> erreurs) {
        if (reference == null || reference.isBlank()) {
            erreurs.add(ErreurImportDTO.builder()
                    .activite(idActivite)
                    .champ("reference")
                    .message("La référence de l'activité est obligatoire.")
                    .build());
            return null;
        }
        Activite act = parReference(reference);
        if (act == null) {
            erreurs.add(ErreurImportDTO.builder()
                    .activite(idActivite)
                    .champ("reference")
                    .message("Aucune activité ne correspond à la référence « "
                            + reference.trim() + " ».")
                    .build());
            return null;
        }
        if (!autorise(act.getService())) {
            erreurs.add(ErreurImportDTO.builder()
                    .activite(idActivite)
                    .champ("reference")
                    .message("Vous n'êtes pas autorisé à modifier cette activité.")
                    .build());
            return null;
        }
        return act;
    }

    public boolean autorise(mg.bank.backend.model.referentiel.Service service) {
        if (service == null) return false;
        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        if (perimetre == null) return false;
        if (!porteeService.serviceAccessible(service)) return false;
        if (perimetre.estRattacheService()) {
            return service.getIdService() != null
                    && service.getIdService().equals(perimetre.getIdService());
        }
        if (perimetre.estNiveauDepartement()) {
            return service.getDepartement() != null
                    && service.getDepartement().getIdDepartement() != null
                    && service.getDepartement().getIdDepartement()
                            .equals(perimetre.getIdDepartement());
        }
        return true;
    }
}