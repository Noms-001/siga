package mg.bank.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ActiviteDetailDTO;
import mg.bank.backend.dto.ActiviteFiltreCriteria;
import mg.bank.backend.dto.ActiviteListItemDTO;
import mg.bank.backend.dto.ActiviteOptionsDTO;
import mg.bank.backend.dto.ActiviteStatistiquesDTO;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.AutocompleteDTO;
import mg.bank.backend.dto.FichierTelechargeable;
import mg.bank.backend.dto.PageResponse;
import mg.bank.backend.dto.SousActiviteDetailDTO;
import mg.bank.backend.service.ActiviteDetailService;
import mg.bank.backend.service.ActiviteService;

@RestController
@RequestMapping("/api/activites")
@RequiredArgsConstructor
public class ActiviteController {

    private final ActiviteService activiteService;

    private final ActiviteDetailService activiteDetailService;

    /**
     * Liste paginee et filtree.
     *
     * Le tri par defaut est deja porte par le repository (recence decroissante
     * sur la date de creation = premiere entree d'historique) ; le tri du
     * front n'est applique que s'il est explicitement demande, et ses
     * colonnes sont filtrees par une liste blanche cote repository.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ActiviteListItemDTO>>> lister(
            @RequestParam(required = false) Boolean pta,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer objectifSpecifiqueId,
            @RequestParam(required = false) String objectifSearch,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer prioriteId,
            @RequestParam(required = false) Integer typeActiviteId,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @PageableDefault(size = 10) Pageable pagination) {

        ActiviteFiltreCriteria filtres = filtres(
                pta, search, objectifSpecifiqueId, objectifSearch, serviceId,
                prioriteId, typeActiviteId, siteId, statut, annee, trimestre,
                dateDebut, dateFin);

        PageResponse<ActiviteListItemDTO> resultat = activiteService.rechercher(
                filtres,
                pagination.getPageNumber(),
                pagination.getPageSize(),
                pagination.getSort());

        return ResponseEntity.ok(ApiResponse.success(resultat));
    }

    /**
     * Meme jeu de filtres que la liste, meme perimetre : les cards ne peuvent
     * pas afficher de compteur incoherent avec le tableau.
     */
    @GetMapping("/statistiques")
    public ResponseEntity<ApiResponse<ActiviteStatistiquesDTO>> statistiques(
            @RequestParam(required = false) Boolean pta,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer objectifSpecifiqueId,
            @RequestParam(required = false) String objectifSearch,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) Integer prioriteId,
            @RequestParam(required = false) Integer typeActiviteId,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin) {

        ActiviteFiltreCriteria filtres = filtres(
                pta, search, objectifSpecifiqueId, objectifSearch, serviceId,
                prioriteId, typeActiviteId, siteId, statut, annee, trimestre,
                dateDebut, dateFin);

        return ResponseEntity.ok(
                ApiResponse.success(activiteService.statistiques(filtres)));
    }

    /**
     * Referentiels des filtres, lus en base.
     *
     * Le tableau de services n'est renvoye qu'a un utilisateur de niveau
     * departement : un utilisateur rattache a un service n'a rien a choisir.
     */
    @GetMapping("/options")
    public ResponseEntity<ApiResponse<ActiviteOptionsDTO>> options() {
        return ResponseEntity.ok(ApiResponse.success(activiteService.options()));
    }

    /**
     * Autocomplete activite : code, reference ou designation.
     * Deja restreint au perimetre de l'utilisateur.
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponse<List<AutocompleteDTO>>> autocomplete(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(
                ApiResponse.success(activiteService.autocompleterActivites(q)));
    }

    /**
     * Autocomplete objectif specifique, reserve a la vue PTA.
     */
    @GetMapping("/objectifs/autocomplete")
    public ResponseEntity<ApiResponse<List<AutocompleteDTO>>> autocompleteObjectifs(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(
                ApiResponse.success(activiteService.autocompleterObjectifs(q)));
    }

    /**
     * Detail complet d une activite.
     *
     * Place apres les routes a parametre pour que /statistiques, /options et
     * /autocomplete ne soient pas captures comme un identifiant. Les motifs
     * {id} de ce controller ne sont pas imposes par une contrainte, donc
     * l ordre des declarations est ici ce qui empeche un 400 inattendu sur une
     * route existante.
     *
     * Le nom de la variable evite de reutiliser id, deja employe par les
     * javax/jakarta id dans les annotations de ce fichier.
     *
     * Restreint au meme perimetre que la liste, sans quoi un utilisateur
     * pourrait atteindre une activite qu il ne voit pas dans le tableau.
     * L identite de l appelant n est pas un parametre : elle vient du contexte
     * de securite, donc elle ne peut pas etre forgee.
     *
     * 404 si l activite n existe pas, 403 si elle existe mais sort du
     * perimetre. Voir ActiviteDetailService pour la raison de cette
     * distinction.
     */
    @GetMapping("/{idActivite}")
    public ResponseEntity<ApiResponse<ActiviteDetailDTO>> detail(
            @PathVariable("idActivite") Integer idActivite) {
        return ResponseEntity.ok(
                ApiResponse.success(activiteDetailService.detail(idActivite)));
    }

    /**
     * Detail d une sous-activite du detail d une activite.
     *
     * Deux niveaux en deux identifiants : la sous-activite doit exister ET
     * appartenir a l activite demandee, elle-meme dans le perimetre de
     * l appelant. 404 si la sous-activite est inconnue ou rattachee a une
     * autre activite, 403 si elle depend d une activite hors perimetre, comme
     * pour le detail parent.
     */
    @GetMapping("/{idActivite}/sous-activites/{idSousActivite}")
    public ResponseEntity<ApiResponse<SousActiviteDetailDTO>> detailSousActivite(
            @PathVariable("idActivite") Integer idActivite,
            @PathVariable("idSousActivite") Integer idSousActivite) {
        return ResponseEntity.ok(
                ApiResponse.success(activiteDetailService.detailSousActivite(
                        idActivite, idSousActivite)));
    }

    /**
     * Contenu binaire d un fichier d une sous-activite.
     *
     * Cet endpoint echappe volontairement a l enveloppe ApiResponse : une
     * reponse JSON ne peut pas porter des octets. Le succes renvoie le
     * fichier brut (Content-Type du depot, disposition inline pour que le
     * navigateur puisse l afficher), les erreurs restent au format JSON
     * habituel via GlobalExceptionHandler.
     *
     * Meme perimetre et meme departage 404/403 que le detail de sous-activite.
     * Le nom vient de la base (nom_original) et est code en UTF-8 dans
     * l en-tete au format RFC 5987.
     */
    @GetMapping("/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}")
    public ResponseEntity<byte[]> telechargerFichier(
            @PathVariable("idActivite") Integer idActivite,
            @PathVariable("idSousActivite") Integer idSousActivite,
            @PathVariable("idFichier") Integer idFichier) {

        FichierTelechargeable fichier = activiteDetailService.telechargerFichier(
                idActivite, idSousActivite, idFichier);

        MediaType type = (fichier.typeMime() != null && !fichier.typeMime().isBlank())
                ? MediaType.parseMediaType(fichier.typeMime())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(type)
                .contentLength(fichier.contenu().length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        dispositionInline(fichier.nomOriginal()))
                .body(fichier.contenu());
    }

    /**
     * Disposition inline avec nom code en UTF-8 (RFC 5987).
     *
     * Un nom de fichier "mise à jour.pdf" ne passe pas tel quel dans un
     * en-tete HTTP : la paire filename="" porte une version ascii (lettres
     * accentuees remplacees par _), et filename* porte la version UTF-8
     * percent-codee que les navigateurs modernes preferent.
     */
    private String dispositionInline(String nom) {

        String assaini = nom
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");

        String ascii = assaini.replaceAll("[^\\x20-\\x7E]", "_");
        String utf8 = URLEncoder.encode(assaini, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return String.format(
                "inline; filename=\"%s\"; filename*=UTF-8''%s",
                ascii, utf8);
    }

    private ActiviteFiltreCriteria filtres(
            Boolean pta,
            String search,
            Integer objectifSpecifiqueId,
            String objectifSearch,
            Integer serviceId,
            Integer prioriteId,
            Integer typeActiviteId,
            Integer siteId,
            String statut,
            Integer annee,
            Integer trimestre,
            LocalDate dateDebut,
            LocalDate dateFin) {

        return ActiviteFiltreCriteria.builder()
                .pta(pta)
                .search(videSiNull(search))
                .objectifSpecifiqueId(objectifSpecifiqueId)
                .objectifSearch(videSiNull(objectifSearch))
                .serviceId(serviceId)
                .prioriteId(prioriteId)
                .typeActiviteId(typeActiviteId)
                .siteId(siteId)
                .statutCode(videSiNull(statut))
                .annee(annee)
                .trimestre(trimestre)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .build();
    }

    private String videSiNull(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }

}
