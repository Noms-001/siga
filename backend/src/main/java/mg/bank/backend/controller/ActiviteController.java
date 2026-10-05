package mg.bank.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ActiviteDetailDTO;
import mg.bank.backend.dto.ActiviteEcritureRequest;
import mg.bank.backend.dto.ActiviteEcritureResponse;
import mg.bank.backend.dto.ActiviteFiltreCriteria;
import mg.bank.backend.dto.ActiviteFormulaireDTO;
import mg.bank.backend.dto.ActiviteListItemDTO;
import mg.bank.backend.dto.ActiviteOptionsDTO;
import mg.bank.backend.dto.ActiviteStatistiquesDTO;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.AutocompleteDTO;
import mg.bank.backend.dto.CodeProposeDTO;
import mg.bank.backend.dto.FichierTelechargeable;
import mg.bank.backend.dto.LivrableAjoutRequest;
import mg.bank.backend.dto.LivrableDetailDTO;
import mg.bank.backend.dto.ObjectifSpecifiqueEcritureRequest;
import mg.bank.backend.dto.PageResponse;
import mg.bank.backend.dto.ReferenceDTO;
import mg.bank.backend.dto.SousActiviteDetailDTO;
import mg.bank.backend.dto.ValidationActiviteResponse;
import mg.bank.backend.model.Activite;
import mg.bank.backend.model.EtapeValidation;
import mg.bank.backend.model.ValidationActivite;
import mg.bank.backend.service.ActiviteDetailService;
import mg.bank.backend.service.ActiviteEcritureService;
import mg.bank.backend.service.ActiviteService;
import mg.bank.backend.service.LivrableEcritureService;
import mg.bank.backend.service.EtapeValidationService;
import mg.bank.backend.service.ValidationActiviteService;

@RestController
@RequestMapping("/api/activites")
@RequiredArgsConstructor
public class ActiviteController {

        private final ActiviteService activiteService;

        private final ActiviteDetailService activiteDetailService;

        private final ActiviteEcritureService activiteEcritureService;
        private final LivrableEcritureService livrableEcritureService;
        private final EtapeValidationService etapeValidationService;
        private final ValidationActiviteService validationActiviteService;

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

        @PostMapping("/{idActivite}/soumettre-validation")
        public ResponseEntity<ApiResponse<ValidationActiviteResponse>> soumettreValidation(
                        @PathVariable Integer idActivite,
                        @RequestParam(defaultValue = "false") Boolean retour,
                        @RequestParam(required = false) String commentaire) {
                ValidationActivite validationActivite = activiteService.soumettre(idActivite, retour, commentaire);
                return ResponseEntity.ok(
                                ApiResponse.success(new ValidationActiviteResponse(validationActivite, null)));
        }

        @PostMapping("/{idActivite}/valider")
        public ResponseEntity<ApiResponse<ValidationActiviteResponse>> valider(
                        @PathVariable Integer idActivite,
                        @RequestParam(defaultValue = "false") Boolean rejeter,
                        @RequestParam(required = false) String commentaire) {
                ValidationActivite validationActivite = activiteService.valider(idActivite, rejeter, commentaire);
                return ResponseEntity.ok(
                                ApiResponse.success(new ValidationActiviteResponse(validationActivite, null)));
        }

        @GetMapping("/a-soumettre")
        public ResponseEntity<ApiResponse<PageResponse<ActiviteListItemDTO>>> getBrouillons() {

                ActiviteService.ContexteSoumission contexte = activiteService.getContexteSoumission();

                /*
                 * Les trois étapes sont communes à toutes les lignes : elles dépendent
                 * du poste de l'utilisateur connecté, pas de chaque activité. On les
                 * construit une seule fois et on les partage par référence -- les DTO
                 * portent chacun un pointeur vers le même objet, pas une copie.
                 */
                ReferenceDTO couranteRef = contexte.etapeCourante() == null ? null
                                : ReferenceDTO.builder()
                                                .id(contexte.etapeCourante().getIdEtapeValidation())
                                                .libelle(contexte.etapeCourante().getDesignation())
                                                .build();

                ReferenceDTO suivanteRef = contexte.etapeSuivante()
                                .map(e -> ReferenceDTO.builder()
                                                .id(e.getIdEtapeValidation())
                                                .libelle(e.getDesignation())
                                                .build())
                                .orElse(null);

                ReferenceDTO precedenteRef = contexte.etapePrecedente()
                                .map(e -> ReferenceDTO.builder()
                                                .id(e.getIdEtapeValidation())
                                                .libelle(e.getDesignation())
                                                .build())
                                .orElse(null);

                List<ActiviteListItemDTO> content = contexte.brouillons().stream()
                                .map(activite -> {
                                        ActiviteListItemDTO dto = new ActiviteListItemDTO(activite);
                                        dto.setEtapeCourante(couranteRef);
                                        dto.setEtapeSuivante(suivanteRef);
                                        dto.setEtapePrecedente(precedenteRef);
                                        return dto;
                                })
                                .toList();

                PageResponse<ActiviteListItemDTO> response = PageResponse.<ActiviteListItemDTO>builder()
                                .content(content)
                                .page(0)
                                .size(content.size())
                                .totalElements(content.size())
                                .totalPages(content.isEmpty() ? 0 : 1)
                                .first(true)
                                .last(true)
                                .build();

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        @GetMapping("/a-valider")
        public ResponseEntity<ApiResponse<PageResponse<Map<String, ActiviteListItemDTO>>>> getNonValider() {

                Map<EtapeValidation, List<Activite>> nonValidees = activiteService.getActiviteNonValidees();

                // 1. Toutes les activités à plat
                List<Activite> activites = nonValidees.values().stream()
                                .flatMap(List::stream)
                                .toList();

                // 2. Batch : décisions précédentes en 1 requête
                List<Integer> idsActivites = activites.stream()
                                .map(Activite::getIdActivite)
                                .toList();
                Map<Integer, ValidationActivite> decisionsPrecedentes = validationActiviteService
                                .getDecisionsPrecedentesParActivite(idsActivites);

                // 3. Précalcul suivante/précédente pour chaque étape (pas par activité)
                List<EtapeValidation> etapes = new ArrayList<>(nonValidees.keySet());
                Map<Integer, EtapeValidationService.EtapesVoisines> voisines = etapeValidationService
                                .getVoisinesParEtapes(etapes);

                // 4. Assemblage en mémoire
                List<Map<String, ActiviteListItemDTO>> content = new ArrayList<>();
                for (Map.Entry<EtapeValidation, List<Activite>> entry : nonValidees.entrySet()) {
                        EtapeValidation etape = entry.getKey();
                        EtapeValidationService.EtapesVoisines v = voisines.get(etape.getIdEtapeValidation());

                        ReferenceDTO etapeCouranteRef = ReferenceDTO.builder()
                                        .id(etape.getIdEtapeValidation())
                                        .libelle(etape.getDesignation())
                                        .build();
                        ReferenceDTO etapeSuivanteRef = v.suivante().map(s -> ReferenceDTO.builder()
                                        .id(s.getIdEtapeValidation()).libelle(s.getDesignation()).build()).orElse(null);
                        ReferenceDTO etapePrecedenteRef = v.precedente().map(p -> ReferenceDTO.builder()
                                        .id(p.getIdEtapeValidation()).libelle(p.getDesignation()).build()).orElse(null);

                        for (Activite activite : entry.getValue()) {
                                ActiviteListItemDTO dto = new ActiviteListItemDTO(activite);
                                dto.setEtapeCourante(etapeCouranteRef);
                                dto.setEtapeSuivante(etapeSuivanteRef);
                                dto.setEtapePrecedente(etapePrecedenteRef);

                                ValidationActivite precedente = decisionsPrecedentes.get(activite.getIdActivite());
                                if (precedente != null) {
                                        dto.setDerniereValidation(new ValidationActiviteResponse(
                                                        precedente,
                                                        precedente.getDecision() != null
                                                                        ? precedente.getDecision().name()
                                                                        : null));
                                }

                                Map<String, ActiviteListItemDTO> map = new HashMap<>(1);
                                map.put(etape.getDesignation(), dto);
                                content.add(map);
                        }
                }

                PageResponse<Map<String, ActiviteListItemDTO>> response = PageResponse
                                .<Map<String, ActiviteListItemDTO>>builder()
                                .content(content)
                                .page(0)
                                .size(content.size())
                                .totalElements(content.size())
                                .totalPages(content.isEmpty() ? 0 : 1)
                                .first(true)
                                .last(true)
                                .build();

                return ResponseEntity.ok(ApiResponse.success(response));
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
         * Creer un objectif specifique, depuis le bouton place a cote du champ.
         *
         * La route est voisine de celle de l'autocomplete, et non une ressource
         * /objectifs-specifique : dans l'application, un objectif se cree en
         * affectant une activite, pas dans un ecran de referentiel a part. Le
         * rattacher a l'activite garde les deux usages voisins.
         *
         * La reponse est l'objectif cree, au format de l'autocomplete, pour que
         * le formulaire le selectionne sans relire la liste.
         *
         * 409 si le code est deja pris, 400 si un champ est absent ou hors bornes.
         */
        @PostMapping("/objectifs")
        public ResponseEntity<ApiResponse<AutocompleteDTO>> creerObjectif(
                        @Valid @RequestBody ObjectifSpecifiqueEcritureRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(
                                                activiteService.creerObjectifSpecifique(request)));
        }

        /**
         * Code propose pour une activite NON PTA.
         *
         * Riche de l'ecriture mais portee par une lecture : rien n est cree, et
         * la reponse est une valeur calculee, pas une ressource. La proposer en
         * POST serait plus regulier, et obligerait a envoyer une annee pour
         * obtenir un nombre.
         *
         * L'annee est en parametre et non deduite : une NON PTA n'a pas d'objectif,
         * donc rien dans l'activite ne la designe avant que l'utilisateur ne
         * renseigne sa date de debut.
         *
         * 400 si l'annee est hors bornes.
         */
        @GetMapping("/non-pta/prochain-code")
        public ResponseEntity<ApiResponse<CodeProposeDTO>> prochainCodeNonPta(
                        @RequestParam("annee") int annee) {
                return ResponseEntity.ok(
                                ApiResponse.success(CodeProposeDTO.builder()
                                                .code(activiteService.prochainCodeNonPta(annee))
                                                .annee(annee)
                                                .build()));
        }

        /**
         * Creer une activite en brouillon.
         *
         * 201 et non 200 : une ressource est creee, et l identifiant qu elle
         * recoit ne peut pas etre connu du client avant l appel. Le statut
         * n est pas dans le corps et n est pas demande : il est pose a
         * BROUILLON par le backend, qui est aussi le seul a decider si
         * l activite creee est reellement un brouillon.
         *
         * Le reponse ne renvoie que ce que le formulaire a besoin de
         * confirmer : identifiant, code, statut, nombre de sous-activites. Le
         * detail complet se lit ensuite sur /{idActivite}.
         */
        @PostMapping
        public ResponseEntity<ApiResponse<ActiviteEcritureResponse>> creer(
                        @Valid @RequestBody ActiviteEcritureRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(activiteEcritureService.creer(request)));
        }

        /**
         * Modifier une activite.
         *
         * PUT et non PATCH parce que le corps est l etat complet de ce que le
         * formulaire edite : une modification partielle donnerait au
         * client le pouvoir d effacer un champ en ne l envoyant pas.
         *
         * Le statut n est pas modifiable, ni ici ni dans le corps : seule une
         * activite en brouillon est acceptee, et elle le reste. Voir
         * ActiviteEcritureService.
         */
        @PutMapping("/{idActivite}")
        public ResponseEntity<ApiResponse<ActiviteEcritureResponse>> modifier(
                        @PathVariable("idActivite") Integer idActivite,
                        @Valid @RequestBody ActiviteEcritureRequest request) {

                return ResponseEntity.ok(
                                ApiResponse.success(activiteEcritureService.modifier(
                                                idActivite, request)));
        }

        /**
         * Ce que le formulaire de modification doit pre-remplir.
         *
         * Sous-ensemble delibere du detail d une activite : le detail porte en
         * plus avancement, livrables, fichiers, indicateurs et
         * historique, qu un formulaire de redaction n edite pas. Servir le
         * detail obligerait le front a deviner quels champs lui appartiennent.
         */
        @GetMapping("/{idActivite}/formulaire")
        public ResponseEntity<ApiResponse<ActiviteFormulaireDTO>> formulaire(
                        @PathVariable("idActivite") Integer idActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteEcritureService.formulaire(idActivite)));
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
         * Deposer un livrable, et ses fichiers, sur une sous-activite.
         *
         * MULTIPART ET NON JSON, POUR UNE RAISON QUI TIENT AU FICHIER
         *
         * Le livrable lui-meme (designation, description) reste du JSON, dans la
         * partie "livrable" : il est ainsi valide par @Valid comme les autres
         * corps de requete, et les messages d'erreur restent nommes. Les octets ne
         * peuvent pas tenir dans un JSON -- il faudrait les encoder -- et
         * multipart les transporte tels quels, sans copie intermediaire.
         *
         * "fichiers" est facultatif : un livrable peut etre declare avant que la
         * piece soit prete. C est une decision de fond, pas une commodite de
         * transport -- voir LivrableEcritureService.
         *
         * Le statut est verifie cote serveur, sous verrou, sur le statut courant de
         * l historique : un client ne peut pas s attribuer le droit d ecrire en
         * envoyant un statut. Le 409 qui en decoule est donc la seule reponse
         * possible a "j'ai vu le bouton", qui n existe de toute facon que si le
         * detail a renvoye statutActivite = EN_COURS.
         */
        @PostMapping(value = "/{idActivite}/sous-activites/{idSousActivite}/livrables", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<LivrableDetailDTO>> ajouterLivrable(
                        @PathVariable("idActivite") Integer idActivite,
                        @PathVariable("idSousActivite") Integer idSousActivite,
                        @Valid @RequestPart("livrable") LivrableAjoutRequest request,
                        @RequestPart(value = "fichiers", required = false) MultipartFile[] fichiers) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(livrableEcritureService.creer(
                                                idActivite, idSousActivite, request, fichiers)));
        }

        /**
         * Joindre des fichiers a un livrable deja declare.
         *
         * CHEMIN DIFFERENT, MEME SERVICE, MEMES GARDIENS
         *
         * Le livrable existe deja et son identifiant entre dans l URL : c est ce
         * qui distingue cette ecriture de la precedente, ou le livrable est cree
         * dans le meme temps que ses fichiers. Tout le reste est volontairement
         * identique -- meme perimetre, meme verrou, meme statut autorise, memes
         * plafonds -- parce qu il s agit du meme acte : deposer une piece sur une
         * activite. Un depot de fichier ne doit pas etre possible dans un etat ou
         * un livrable ne l est pas.
         *
         * "fichiers" est ici OBLIGATOIRE, la ou il est facultatif a la creation.
         * La difference n est pas un detail de transport : sans fichier, cette
         * requete n ecrirait rien et renverrait un livrable identique a celui de
         * la demande. Elle doit donc etre refusee par le serveur, et non repondue
         * par un succes sans effet.
         *
         * Pas de @Valid ni de partie JSON ici : il n y a aucun champ a valider, les
         * seuls octets etant les fichiers. Le statut reste verifie cote serveur,
         * sous verrou, sur le statut courant de l historique.
         */
        @PostMapping(value = "/{idActivite}/sous-activites/{idSousActivite}/livrables"
                        + "/{idLivrable}/fichiers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<LivrableDetailDTO>> ajouterFichiersLivrable(
                        @PathVariable("idActivite") Integer idActivite,
                        @PathVariable("idSousActivite") Integer idSousActivite,
                        @PathVariable("idLivrable") Integer idLivrable,
                        @RequestPart("fichiers") MultipartFile[] fichiers) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(livrableEcritureService.ajouterFichiers(
                                                idActivite, idSousActivite, idLivrable, fichiers)));
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
