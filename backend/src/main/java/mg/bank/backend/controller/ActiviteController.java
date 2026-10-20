package mg.bank.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import mg.bank.backend.dto.ActiviteSuiviDTO;
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
import mg.bank.backend.service.EtapeValidationService;
import mg.bank.backend.service.LivrableEcritureService;
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

        @PostMapping("/objectifs")
        public ResponseEntity<ApiResponse<AutocompleteDTO>> creerObjectif(
                        @Valid @RequestBody ObjectifSpecifiqueEcritureRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(
                                                activiteService.creerObjectifSpecifique(request)));
        }

        @GetMapping("/non-pta/prochain-code")
        public ResponseEntity<ApiResponse<CodeProposeDTO>> prochainCodeNonPta(
                        @RequestParam("annee") int annee) {
                return ResponseEntity.ok(
                                ApiResponse.success(CodeProposeDTO.builder()
                                                .code(activiteService.prochainCodeNonPta(annee))
                                                .annee(annee)
                                                .build()));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<ActiviteEcritureResponse>> creer(
                        @Valid @RequestBody ActiviteEcritureRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(activiteEcritureService.creer(request)));
        }

        @PutMapping("/{idActivite}")
        public ResponseEntity<ApiResponse<ActiviteEcritureResponse>> modifier(
                        @PathVariable("idActivite") Integer idActivite,
                        @Valid @RequestBody ActiviteEcritureRequest request) {

                return ResponseEntity.ok(
                                ApiResponse.success(activiteEcritureService.modifier(
                                                idActivite, request)));
        }

        @GetMapping("/{idActivite}/formulaire")
        public ResponseEntity<ApiResponse<ActiviteFormulaireDTO>> formulaire(
                        @PathVariable("idActivite") Integer idActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteEcritureService.formulaire(idActivite)));
        }

        @GetMapping("/{idActivite}")
        public ResponseEntity<ApiResponse<ActiviteDetailDTO>> detail(
                        @PathVariable("idActivite") Integer idActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteDetailService.detail(idActivite)));
        }

        @GetMapping("/{idActivite}/sous-activites/{idSousActivite}")
        public ResponseEntity<ApiResponse<SousActiviteDetailDTO>> detailSousActivite(
                        @PathVariable("idActivite") Integer idActivite,
                        @PathVariable("idSousActivite") Integer idSousActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteDetailService.detailSousActivite(
                                                idActivite, idSousActivite)));
        }

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

        @GetMapping("/suivi")
        public ResponseEntity<ApiResponse<PageResponse<ActiviteSuiviDTO>>> suivi(
                        @RequestParam(required = false) Boolean pta,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) Integer serviceId,
                        @RequestParam(required = false) Integer prioriteId,
                        @RequestParam(required = false) String statut,
                        @RequestParam(required = false) Integer annee,
                        @RequestParam(required = false) Integer trimestre,
                        @RequestParam(required = false) LocalDate dateDebut,
                        @RequestParam(required = false) LocalDate dateFin,
                        @PageableDefault(size = 20) Pageable pagination) {

                ActiviteFiltreCriteria filtres = filtres(
                                pta, search, null, null, serviceId,
                                prioriteId, null, null, statut, annee, trimestre,
                                dateDebut, dateFin);

                PageResponse<ActiviteSuiviDTO> resultat = activiteService.rechercherSuivi(
                                filtres,
                                pagination.getPageNumber(),
                                pagination.getPageSize(),
                                pagination.getSort());

                return ResponseEntity.ok(ApiResponse.success(resultat));
        }

}
