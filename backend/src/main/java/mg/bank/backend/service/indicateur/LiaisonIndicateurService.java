package mg.bank.backend.service.indicateur;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.indicateur.IndicateurSimpleDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.indicateur.Indicateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.indicateur.IndicateurRepository;
import mg.bank.backend.repository.projection.common.StatutCourantRow;
import mg.bank.backend.service.activite.ActiviteService;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@Service
@RequiredArgsConstructor
public class LiaisonIndicateurService {

    /** Statuts sur lesquels aucune modification de liaison n'est autorisee. */
    private static final Set<String> STATUTS_VERROUILLES =
            Set.of("TERMINEE", "ANNULEE");

    private final ActiviteRepository activiteRepository;
    private final IndicateurRepository indicateurRepository;
    private final ActiviteService activiteService;
    private final CurrentUserService currentUserService;

    // ------------------------------------------------------------------
    // F8a — Lier
    // ------------------------------------------------------------------

    @Transactional
    public IndicateurSimpleDTO lier(Integer idActivite, Integer idIndicateur) {
        chargerActiviteModifiable(idActivite);

        Indicateur indicateur = chargerIndicateurActif(idIndicateur);

        if (activiteRepository.existsLiaison(idActivite, idIndicateur) > 0) {
            throw new ApiException(
                    "Cet indicateur est déjà lié à cette activité.",
                    HttpStatus.CONFLICT);
        }

        activiteRepository.lierIndicateur(idActivite, idIndicateur);
        return versSimpleDto(indicateur);
    }

    // ------------------------------------------------------------------
    // F8b — Délier
    // ------------------------------------------------------------------

    @Transactional
    public void delier(Integer idActivite, Integer idIndicateur) {
        chargerActiviteModifiable(idActivite);

        if (activiteRepository.existsLiaison(idActivite, idIndicateur) == 0) {
            throw new ApiException(
                    "Cet indicateur n'est pas lié à cette activité.",
                    HttpStatus.NOT_FOUND);
        }

        activiteRepository.delierIndicateur(idActivite, idIndicateur);
    }

    // ------------------------------------------------------------------
    // F8c — Liste des indicateurs disponibles
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<IndicateurSimpleDTO> disponibles(Integer idActivite, String recherche) {
        chargerActiviteDansPerimetre(idActivite);

        String motif = (recherche == null || recherche.isBlank())
                ? null
                : "%" + recherche.trim() + "%";

        return indicateurRepository
                .findDisponiblesPourActivite(idActivite, motif)
                .stream()
                .map(this::versSimpleDto)
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Verifie que l'activite existe, est dans le perimetre, et est modifiable. */
    private Activite chargerActiviteModifiable(Integer idActivite) {
        Activite activite = chargerActiviteDansPerimetre(idActivite);

        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        StatutCourantRow statut = activiteRepository.findStatutCourant(
                        idActivite,
                        perimetre.getFiltreService(),
                        perimetre.getFiltreDepartement())
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        if (statut.getStatutCode() != null
                && STATUTS_VERROUILLES.contains(statut.getStatutCode())) {
            throw new ApiException(
                    "Les indicateurs d'une activité " + statut.getStatutCode().toLowerCase()
                            + " ne peuvent plus être modifiés.",
                    HttpStatus.CONFLICT);
        }
        return activite;
    }

    private Activite chargerActiviteDansPerimetre(Integer idActivite) {
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        Integer servicePerimetre = perimetre.getFiltreService();
        Integer departementPerimetre = perimetre.getFiltreDepartement();

        if (activiteRepository.findActiviteDetail(
                idActivite, servicePerimetre, departementPerimetre,
                String.join(",", mg.bank.backend.enums.StatutActiviteEnum.codesTerminaux()))
                .isEmpty()) {
            throw activiteService.refusOuAbsence(idActivite);
        }
        return activiteRepository.findById(idActivite)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));
    }

    private Indicateur chargerIndicateurActif(Integer idIndicateur) {
        Indicateur indicateur = indicateurRepository.findById(idIndicateur)
                .orElseThrow(() -> new ApiException(
                        "Indicateur introuvable.", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(indicateur.getActif())) {
            throw new ApiException(
                    "L'indicateur n'est pas actif : il ne peut pas être lié.",
                    HttpStatus.BAD_REQUEST);
        }
        return indicateur;
    }

    private IndicateurSimpleDTO versSimpleDto(Indicateur i) {
        return IndicateurSimpleDTO.builder()
                .id(i.getIdIndicateur())
                .code(i.getCode())
                .designation(i.getDesignation())
                .typeIndicateur(i.getTypeIndicateur())
                .uniteMesure(i.getUniteMesure())
                .actif(i.getActif())
                .build();
    }
}