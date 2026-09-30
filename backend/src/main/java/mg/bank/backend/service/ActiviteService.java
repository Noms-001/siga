package mg.bank.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ActiviteFiltreCriteria;
import mg.bank.backend.dto.ActiviteListItemDTO;
import mg.bank.backend.dto.ActiviteOptionsDTO;
import mg.bank.backend.dto.ActiviteStatistiquesDTO;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.AutocompleteDTO;
import mg.bank.backend.dto.OptionDTO;
import mg.bank.backend.dto.PageResponse;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.dto.ReferenceDTO;
import mg.bank.backend.enums.StatutActiviteEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.ObjectifSpecifiqueRepository;
import mg.bank.backend.repository.PrioriteRepository;
import mg.bank.backend.repository.UtilisateurRepository;
import mg.bank.backend.repository.ServiceRepository;
import mg.bank.backend.repository.SiteRepository;
import mg.bank.backend.repository.StatutRepository;
import mg.bank.backend.repository.TypeActiviteRepository;
import mg.bank.backend.repository.projection.ActiviteListRow;
import mg.bank.backend.repository.projection.ActiviteStatistiquesRow;
import mg.bank.backend.repository.projection.OptionRow;

/**
 * Service de consultation des activites.
 *
 * Toute la logique metier vit ici : les repositories ne contiennent que les
 * requetes. C'est notamment le cas de trois decisions :
 *
 * 1. le perimetre, reconstruit depuis l'utilisateur authentifie et jamais
 *    depuis un parametre HTTP ;
 * 2. les bornes de mois du trimestre, calculees ici pour que le SQL reste
 *    sappable sur date_debut_prevue ;
 * 3. la liste des codes terminaux, injectee dans le SQL sous forme de
 *    liste chainee puis scindee par la base.
 */
@Service
@RequiredArgsConstructor
public class ActiviteService {

    private static final int TAILLE_PAGE_MAX = 100;
    private static final int TAILLE_AUTOCOMPLETE = 10;

    /**
     * Tri par defaut : de la plus recente a la plus ancienne.
     *
     * La colonne dateCreationTri n'est jamais nulle (COALESCE fait le
     * travail en base), donc pas besoin de NULLS LAST.
     */
    private static final Sort TRI_PAR_DEFAUT =
            Sort.by(Sort.Direction.DESC, "dateCreationTri");

    private final ActiviteRepository activiteRepository;
    private final ObjectifSpecifiqueRepository objectifSpecifiqueRepository;
    private final PrioriteRepository prioriteRepository;
    private final TypeActiviteRepository typeActiviteRepository;
    private final SiteRepository siteRepository;
    private final StatutRepository statutRepository;
    private final ServiceRepository serviceRepository;
    private final UtilisateurRepository utilisateurRepository;

    // ------------------------------------------------------------------
    // Perimetre
    // ------------------------------------------------------------------

    /**
     * Perimetre de l'utilisateur connecte, derive de ses rattachements.
     *
     * - service et departement : bloque sur son service
     * - departement seul : restreint a son departement
     * - aucun : pas de restriction
     */
    @Transactional(readOnly = true)
    public PerimetreUtilisateur getPerimetre() {
        Utilisateur utilisateur = utilisateurCourant();

        Integer idService = utilisateur.getService() == null
                ? null
                : utilisateur.getService().getIdService();

        Integer idDepartement = utilisateur.getDepartement() == null
                ? null
                : utilisateur.getDepartement().getIdDepartement();

        return PerimetreUtilisateur.builder()
                .idService(idService)
                .idDepartement(idDepartement)
                .build();
    }

    private Utilisateur utilisateurCourant() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ApiException("Utilisateur non authentifie", HttpStatus.UNAUTHORIZED);
        }

        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException(
                        "Utilisateur introuvable", HttpStatus.UNAUTHORIZED));
    }

    // ------------------------------------------------------------------
    // Conversion des criteres en parametres de requete
    // ------------------------------------------------------------------

    /**
     * Prepared les arguments des requetes.
     *
     * Regle appliquee ici : un utilisateur rattache a un service ne peut pas
     * elargir son perimetre. Son service est envoye comme servicePerimetre,
     * et une selection de service differente est transmise telle quelle :
     * les deux contraintes se cumuleront en base et ne renverront rien,
     * plutot que d'ignorer silencieusement la tentative.
     */
    private record Arguments(
            Boolean pta,
            String recherche,
            Integer objectifId,
            String objectifRecherche,
            Integer servicePerimetre,
            Integer departementPerimetre,
            Integer serviceDemande,
            Integer prioriteId,
            Integer typeActiviteId,
            Integer siteId,
            String statutCode,
            String codesTerminaux,
            Integer annee,
            Integer moisDebut,
            Integer moisFin,
            LocalDate dateDebut,
            LocalDate dateFin) {
    }

    private Arguments arguments(ActiviteFiltreCriteria filtres, PerimetreUtilisateur perimetre) {

        // La vue PTA est le defaut : une activite sans objectif n'est pas
        // cherchee sauf demande explicite de la vue NON PTA.
        Boolean pta = filtres == null || filtres.getPta() == null
                ? Boolean.TRUE
                : filtres.getPta();

        // Hors vue PTA, un objectif ne peut pas etre recherche : il en
        // existe par definition. On l'ignore plutot que de le transmettre.
        boolean horsPta = Boolean.FALSE.equals(pta);

        Integer servicePerimetre = perimetre.estRattacheService()
                ? perimetre.getIdService()
                : null;

        Integer departementPerimetre = perimetre.estNiveauDepartement()
                ? perimetre.getIdDepartement()
                : null;

        Integer serviceDemande = filtres == null ? null : filtres.getServiceId();

        // Un utilisateur de niveau departement ne peut selectionner qu'un
        // service de son departement ; c'est deja garanti par la contrainte
        // de departement appliquee en base.

        return new Arguments(
                pta,
                motif(filtres == null ? null : filtres.getSearch()),
                horsPta ? null : (filtres == null ? null : filtres.getObjectifSpecifiqueId()),
                horsPta ? null : motif(filtres == null ? null : filtres.getObjectifSearch()),
                servicePerimetre,
                departementPerimetre,
                serviceDemande,
                filtres == null ? null : filtres.getPrioriteId(),
                filtres == null ? null : filtres.getTypeActiviteId(),
                filtres == null ? null : filtres.getSiteId(),
                videSiBlanc(filtres == null ? null : filtres.getStatutCode()),
                codesTerminaux(),
                filtres == null ? null : filtres.getAnnee(),
                moisDebut(filtres == null ? null : filtres.getTrimestre()),
                moisFin(filtres == null ? null : filtres.getTrimestre()),
                filtres == null ? null : filtres.getDateDebut(),
                filtres == null ? null : filtres.getDateFin());
    }

    /**
     * Convertit un trimestre en bornes de mois.
     * T1 = janvier a mars, T2 = avril a juin, T3 = juillet a septembre,
     * T4 = octobre a decembre.
     */
    private Integer moisDebut(Integer trimestre) {
        if (trimestre == null || trimestre < 1 || trimestre > 4) {
            return null;
        }
        return (trimestre - 1) * 3 + 1;
    }

    private Integer moisFin(Integer trimestre) {
        if (trimestre == null || trimestre < 1 || trimestre > 4) {
            return null;
        }
        return (trimestre - 1) * 3 + 3;
    }

    /**
     * Codes terminaux, injectes dans le SQL.
     * Le service est seul a decider quels statuts terminent une activite.
     */
    private String codesTerminaux() {
        return String.join(",", StatutActiviteEnum.codesTerminaux());
    }

    /** Un filtre vide n'est pas transmis : il vaut "pas de contrainte". */
    private String motif(String recherche) {
        String valeur = videSiBlanc(recherche);
        return valeur == null ? null : "%" + valeur + "%";
    }

    private String videSiBlanc(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }

    // ------------------------------------------------------------------
    // Liste
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ActiviteListItemDTO> rechercher(
            ActiviteFiltreCriteria filtres,
            int page,
            int size,
            Sort sort) {

        if (page < 0) {
            throw new ApiException(
                    "Le numero de page ne peut pas etre negatif",
                    HttpStatus.BAD_REQUEST);
        }

        if (size < 1 || size > TAILLE_PAGE_MAX) {
            throw new ApiException(
                    "La taille de page doit etre comprise entre 1 et " + TAILLE_PAGE_MAX,
                    HttpStatus.BAD_REQUEST);
        }

        PerimetreUtilisateur perimetre = getPerimetre();
        Arguments args = arguments(filtres, perimetre);

        // Un tri explicite du client est accepte, mais la colonne est
        // controlee par liste blanche dans le nom d'alias ci-dessous.
        Sort tri = (sort == null || sort.isUnsorted()) ? TRI_PAR_DEFAUT : sort;

        Pageable pageable = PageRequest.of(page, size, assainir(tri));

        Page<ActiviteListItemDTO> resultat = activiteRepository.findActivites(
                        args.pta(),
                        args.recherche(),
                        args.objectifId(),
                        args.objectifRecherche(),
                        args.servicePerimetre(),
                        args.departementPerimetre(),
                        args.serviceDemande(),
                        args.prioriteId(),
                        args.typeActiviteId(),
                        args.siteId(),
                        args.statutCode(),
                        args.codesTerminaux(),
                        args.annee(),
                        args.moisDebut(),
                        args.moisFin(),
                        args.dateDebut(),
                        args.dateFin(),
                        pageable)
                .map(this::versDto);

        return PageResponse.from(resultat);
    }

    /**
     * Ne conserve que les colonnes triables declarees dans la requete.
     * Un nom de colonne venant de l'exterieur ne doit jamais atteindre le SQL.
     */
    private Sort assainir(Sort sort) {
        List<String> autorisees = List.of(
                "dateCreationTri", "code", "reference", "designation",
                "dateDebutPrevue", "dateFinPrevue", "avancement", "statutCode");

        List<Sort.Order> retenus = sort.stream()
                .filter(ordre -> autorisees.contains(ordre.getProperty()))
                .toList();

        return retenus.isEmpty() ? TRI_PAR_DEFAUT : Sort.by(retenus);
    }

    // ------------------------------------------------------------------
    // Statistiques
    // ------------------------------------------------------------------

    /**
     * Compteurs des cards.
     *
     * Le filtre de statut n'est volontairement pas transmis : sinon cliquer
     * sur une card mettrait les autres a zero. Les cards refletent les
     * autres filtres, et la card active est signalee par le filtre qu'elle
     * pose sur le tableau.
     */
    @Transactional(readOnly = true)
    public ActiviteStatistiquesDTO statistiques(ActiviteFiltreCriteria filtres) {

        PerimetreUtilisateur perimetre = getPerimetre();
        Arguments args = arguments(filtres, perimetre);

        ActiviteStatistiquesRow row = activiteRepository.statistiques(
                args.pta(),
                args.recherche(),
                args.objectifId(),
                args.objectifRecherche(),
                args.servicePerimetre(),
                args.departementPerimetre(),
                args.serviceDemande(),
                args.prioriteId(),
                args.typeActiviteId(),
                args.siteId(),
                null,
                args.codesTerminaux(),
                args.annee(),
                args.moisDebut(),
                args.moisFin(),
                args.dateDebut(),
                args.dateFin());

        return ActiviteStatistiquesDTO.builder()
                .total(nulSiAbsent(row == null ? null : row.getTotal()))
                .nonCommencees(nulSiAbsent(row == null ? null : row.getNonCommencees()))
                .enCours(nulSiAbsent(row == null ? null : row.getEnCours()))
                .terminees(nulSiAbsent(row == null ? null : row.getTerminees()))
                .enRetard(nulSiAbsent(row == null ? null : row.getEnRetard()))
                .annulees(nulSiAbsent(row == null ? null : row.getAnnulees()))
                .reportees(nulSiAbsent(row == null ? null : row.getReportees()))
                .suspendues(nulSiAbsent(row == null ? null : row.getSuspendues()))
                .build();
    }

    private long nulSiAbsent(Long valeur) {
        return valeur == null ? 0L : valeur;
    }

    // ------------------------------------------------------------------
    // Referentiels de filtres
    // ------------------------------------------------------------------

    /**
     * Referentiels des filtres, lus en base.
     *
     * Les services ne sont fournis qu'a un utilisateur de niveau departement.
     * Un utilisateur rattache a un service recoit un tableau vide : le
     * filtre n'est pas masque par le front, il n'est pas fourni.
     */
    @Transactional(readOnly = true)
    public ActiviteOptionsDTO options() {

        PerimetreUtilisateur perimetre = getPerimetre();

        List<OptionDTO> services = perimetre.estNiveauDepartement()
                ? versOptions(serviceRepository
                        .findOptionsActivesParDepartement(perimetre.getIdDepartement()))
                : List.of();

        return ActiviteOptionsDTO.builder()
                .services(services)
                .priorites(versOptions(prioriteRepository.findOptionsActives()))
                .typesActivite(versOptions(typeActiviteRepository.findOptionsActives()))
                .sites(versOptions(siteRepository.findOptionsActives()))
                .statuts(versOptions(statutRepository.findOptionsActifs()))
                .build();
    }

    private List<OptionDTO> versOptions(List<OptionRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(r -> OptionDTO.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .libelle(r.getLibelle())
                        .build())
                .toList();
    }

    // ------------------------------------------------------------------
    // Autocompletes
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AutocompleteDTO> autocompleterActivites(String recherche) {

        PerimetreUtilisateur perimetre = getPerimetre();

        List<OptionRow> rows = activiteRepository.autocomplete(
                videSiBlanc(recherche),
                motif(recherche),
                perimetre.estRattacheService() ? perimetre.getIdService() : null,
                perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null,
                TAILLE_AUTOCOMPLETE);

        if (rows == null) {
            return List.of();
        }

        return rows.stream()
                .map(r -> AutocompleteDTO.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .libelle(r.getLibelle())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AutocompleteDTO> autocompleterObjectifs(String recherche) {

        List<OptionRow> rows = objectifSpecifiqueRepository.autocomplete(
                videSiBlanc(recherche),
                motif(recherche),
                TAILLE_AUTOCOMPLETE);

        if (rows == null) {
            return List.of();
        }

        return rows.stream()
                .map(r -> AutocompleteDTO.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .libelle(r.getLibelle())
                        .build())
                .toList();
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    private ActiviteListItemDTO versDto(ActiviteListRow row) {
        return ActiviteListItemDTO.builder()
                .id(row.getId())
                .code(row.getCode())
                .reference(row.getReference())
                .designation(row.getDesignation())
                .dateDebutPrevue(row.getDateDebutPrevue())
                .dateFinPrevue(row.getDateFinPrevue())
                .dateDebutReelle(row.getDateDebutReelle())
                .dateFinReelle(row.getDateFinReelle())
                .dateCreation(row.getDateCreation())
                .objectifSpecifique(row.getOsId() == null ? null : ReferenceDTO.builder()
                        .id(row.getOsId())
                        .code(row.getOsCode())
                        .libelle(row.getOsDesignation())
                        .annee(row.getOsAnnee())
                        .build())
                .service(ReferenceDTO.builder()
                        .id(row.getServiceId())
                        .libelle(row.getServiceLibelle())
                        .build())
                .typeActivite(row.getTypeActiviteId() == null ? null : ReferenceDTO.builder()
                        .id(row.getTypeActiviteId())
                        .libelle(row.getTypeActiviteLibelle())
                        .build())
                .site(row.getSiteId() == null ? null : ReferenceDTO.builder()
                        .id(row.getSiteId())
                        .libelle(row.getSiteLibelle())
                        .build())
                .priorite(row.getPrioriteId() == null ? null : ReferenceDTO.builder()
                        .id(row.getPrioriteId())
                        .code(row.getPrioriteCode())
                        .libelle(row.getPrioriteLibelle())
                        .build())
                .statut(row.getStatutId() == null ? null : ReferenceDTO.builder()
                        .id(row.getStatutId())
                        .code(row.getStatutCode())
                        .libelle(row.getStatutLibelle())
                        .build())
                .avancement(row.getAvancement() == null ? 0d : row.getAvancement())
                .build();
    }

}
