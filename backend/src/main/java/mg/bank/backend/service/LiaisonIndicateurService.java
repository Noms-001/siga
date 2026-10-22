package mg.bank.backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.dto.indicateur.IndicateurSimpleDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Activite;
import mg.bank.backend.model.Indicateur;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.IndicateurRepository;
import mg.bank.backend.repository.projection.StatutCourantRow;

/**
 * Lie et delie des indicateurs a une activite.
 *
 * <p>Regles :</p>
 * <ul>
 *   <li>l'activite doit etre dans le perimetre de l'appelant ;</li>
 *   <li>l'activite ne doit pas etre TERMINEE ni ANNULEE ;</li>
 *   <li>l'indicateur doit exister et etre actif ;</li>
 *   <li>la liaison ne doit pas deja exister ;</li>
 *   <li>aucun filtre de perimetre n'est applique sur l'indicateur : si
 *       l'utilisateur peut modifier l'activite, il peut choisir n'importe
 *       quel indicateur actif.</li>
 * </ul>
 *
 * <p>Le deliage est autorise meme si des valeurs ont ete saisies : celles-ci
 * restent rattachees a l'indicateur (pas a la liaison), et reapparaitront si
 * l'indicateur est lie a une autre activite.</p>
 */
@Service
@RequiredArgsConstructor
public class LiaisonIndicateurService {

    /** Statuts sur lesquels aucune modification de liaison n'est autorisee. */
    private static final Set<String> STATUTS_VERROUILLES =
            Set.of("TERMINEE", "ANNULEE");

    private final ActiviteRepository activiteRepository;
    private final IndicateurRepository indicateurRepository;
    private final ActiviteService activiteService;

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

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        StatutCourantRow statut = activiteRepository.findStatutCourant(
                        idActivite,
                        perimetre.estRattacheService() ? perimetre.getIdService() : null,
                        perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null)
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
        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        Integer servicePerimetre = perimetre.estRattacheService() ? perimetre.getIdService() : null;
        Integer departementPerimetre = perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null;

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