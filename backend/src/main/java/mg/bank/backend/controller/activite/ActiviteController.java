package mg.bank.backend.controller.activite;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import mg.bank.backend.dto.activite.ActiviteDetailDTO;
import mg.bank.backend.dto.activite.ActiviteEcritureRequest;
import mg.bank.backend.dto.activite.ActiviteEcritureResponse;
import mg.bank.backend.dto.activite.ActiviteFormulaireDTO;
import mg.bank.backend.dto.activite.ActiviteListItemDTO;
import mg.bank.backend.dto.activite.ActiviteOptionsDTO;
import mg.bank.backend.dto.activite.ActiviteStatistiquesDTO;
import mg.bank.backend.dto.activite.ActiviteSuiviDTO;
import mg.bank.backend.dto.activite.ActiviteSuiviStatistiquesDTO;
import mg.bank.backend.dto.activite.ActiviteTransitionResponse;
import mg.bank.backend.dto.activite.AutocompleteDTO;
import mg.bank.backend.dto.activite.ChangerPrioriteRequest;
import mg.bank.backend.dto.activite.CodeProposeDTO;
import mg.bank.backend.dto.activite.FichierTelechargeable;
import mg.bank.backend.dto.activite.LivrableAjoutRequest;
import mg.bank.backend.dto.activite.LivrableDetailDTO;
import mg.bank.backend.dto.activite.ObjectifSpecifiqueEcritureRequest;
import mg.bank.backend.dto.activite.SousActiviteDetailDTO;
import mg.bank.backend.dto.common.ActiviteFiltreCriteria;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.dto.common.PageResponse;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.dto.export.ActiviteExportResponse;
import mg.bank.backend.dto.validation.ValidationActiviteResponse;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.validation.EtapeValidation;
import mg.bank.backend.model.validation.ValidationActivite;
import mg.bank.backend.service.activite.ActiviteDetailService;
import mg.bank.backend.service.activite.ActiviteEcritureService;
import mg.bank.backend.service.activite.ActiviteExportService;
import mg.bank.backend.service.activite.ActiviteService;
import mg.bank.backend.service.activite.ActiviteWorkflowService;
import mg.bank.backend.service.activite.LivrableEcritureService;
import mg.bank.backend.service.validation.EtapeValidationService;
import mg.bank.backend.service.validation.ValidationActiviteService;
import mg.bank.backend.utils.FileNames;
import mg.bank.backend.security.PermissionAction;
import mg.bank.backend.security.PermissionResource;
import mg.bank.backend.security.RequirePermission;

@RestController
@RequestMapping("/api/activites")
@RequiredArgsConstructor
public class ActiviteController {

        private final ActiviteService activiteService;

        private final ActiviteExportService activiteExportService;

        private final ActiviteDetailService activiteDetailService;

        private final ActiviteEcritureService activiteEcritureService;
        private final LivrableEcritureService livrableEcritureService;
        private final EtapeValidationService etapeValidationService;
        private final ValidationActiviteService validationActiviteService;
        private final ActiviteWorkflowService activiteWorkflowService;

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.DESACTIVER)
        @DeleteMapping("/{idActivite}")
        public ResponseEntity<ApiResponse<Void>> supprimer(@PathVariable Integer idActivite) {
                activiteService.supprimerBrouillon(idActivite);
                return ResponseEntity.ok(ApiResponse.success(null));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
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

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.EXPORTER)
        @GetMapping("/export")
        public ResponseEntity<ApiResponse<ActiviteExportResponse>> exporter(
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

                return ResponseEntity.ok(ApiResponse.success(
                                activiteExportService.exporter(filtres)));
        }

        /**
         * Meme jeu de filtres que la liste, meme perimetre : les cards ne peuvent
         * pas afficher de compteur incoherent avec le tableau.
         */
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
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

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/a-demarrer")
        public ResponseEntity<ApiResponse<PageResponse<ActiviteListItemDTO>>> aDemarrer(
                        @RequestParam(required = false) Boolean pta,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) Integer objectifSpecifiqueId,
                        @RequestParam(required = false) String objectifSearch,
                        @RequestParam(required = false) Integer serviceId,
                        @RequestParam(required = false) Integer prioriteId,
                        @RequestParam(required = false) Integer typeActiviteId,
                        @RequestParam(required = false) Integer siteId,
                        @RequestParam(required = false) Integer annee,
                        @RequestParam(required = false) Integer trimestre,
                        @RequestParam(required = false) LocalDate dateDebut,
                        @RequestParam(required = false) LocalDate dateFin,
                        @PageableDefault(size = 20) Pageable pagination) {

                ActiviteFiltreCriteria filtres = filtres(
                                pta, search, objectifSpecifiqueId, objectifSearch, serviceId,
                                prioriteId, typeActiviteId, siteId, null, annee, trimestre,
                                dateDebut, dateFin);

                return ResponseEntity.ok(ApiResponse.success(
                                activiteService.rechercherADemarrer(
                                                filtres, pagination.getPageNumber(),
                                                pagination.getPageSize(), pagination.getSort())));
        }

        /**
         * Referentiels des filtres, lus en base.
         *
         * Le tableau de services n'est renvoye qu'a un utilisateur de niveau
         * departement : un utilisateur rattache a un service n'a rien a choisir.
         */
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/options")
        public ResponseEntity<ApiResponse<ActiviteOptionsDTO>> options() {
                return ResponseEntity.ok(ApiResponse.success(activiteService.options()));
        }

        /**
         * Autocomplete activite : code, reference ou designation.
         * Deja restreint au perimetre de l'utilisateur.
         */
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/autocomplete")
        public ResponseEntity<ApiResponse<List<AutocompleteDTO>>> autocomplete(
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) Boolean pta) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteService.autocompleterActivites(q, pta)));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.MODIFIER)
        @PostMapping("/{idActivite}/soumettre-validation")
        public ResponseEntity<ApiResponse<ValidationActiviteResponse>> soumettreValidation(
                        @PathVariable Integer idActivite,
                        @RequestParam(defaultValue = "false") Boolean retour,
                        @RequestParam(required = false) String commentaire) {
                ValidationActivite validationActivite = activiteService.soumettre(idActivite, retour, commentaire);
                return ResponseEntity.ok(
                                ApiResponse.success(new ValidationActiviteResponse(validationActivite, null)));
        }

        @RequirePermission(ressource = PermissionResource.VALIDATION_ACTIVITE, action = PermissionAction.VALIDER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.VALIDER)
        @PostMapping("/{idActivite}/valider")
        public ResponseEntity<ApiResponse<ValidationActiviteResponse>> valider(
                        @PathVariable Integer idActivite,
                        @RequestParam(defaultValue = "false") Boolean rejeter,
                        @RequestParam(required = false) String commentaire) {
                ValidationActivite validationActivite = activiteService.valider(idActivite, rejeter, commentaire);
                return ResponseEntity.ok(
                                ApiResponse.success(new ValidationActiviteResponse(validationActivite, null)));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
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

        @RequirePermission(ressource = PermissionResource.VALIDATION_ACTIVITE, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
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
        @RequirePermission(ressource = PermissionResource.OBJECTIF_SPECIFIQUE, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/objectifs/autocomplete")
        public ResponseEntity<ApiResponse<List<AutocompleteDTO>>> autocompleteObjectifs(
                        @RequestParam(required = false) String q) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteService.autocompleterObjectifs(q)));
        }

        @RequirePermission(ressource = PermissionResource.OBJECTIF_SPECIFIQUE, action = PermissionAction.CREER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CREER)
        @PostMapping("/objectifs")
        public ResponseEntity<ApiResponse<AutocompleteDTO>> creerObjectif(
                        @Valid @RequestBody ObjectifSpecifiqueEcritureRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(
                                                activiteService.creerObjectifSpecifique(request)));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/non-pta/prochain-code")
        public ResponseEntity<ApiResponse<CodeProposeDTO>> prochainCodeNonPta(
                        @RequestParam("annee") int annee,
                        @RequestParam(value = "idService", required = false) Integer idService) {
                return ResponseEntity.ok(
                                ApiResponse.success(CodeProposeDTO.builder()
                                                .code(activiteService.prochainCodeNonPta(annee, idService))
                                                .annee(annee)
                                                .build()));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CREER)
        @PostMapping
        public ResponseEntity<ApiResponse<ActiviteEcritureResponse>> creer(
                        @Valid @RequestBody ActiviteEcritureRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(activiteEcritureService.creer(request)));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.MODIFIER)
        @PutMapping("/{idActivite}")
        public ResponseEntity<ApiResponse<ActiviteEcritureResponse>> modifier(
                        @PathVariable("idActivite") Integer idActivite,
                        @Valid @RequestBody ActiviteEcritureRequest request) {

                return ResponseEntity.ok(
                                ApiResponse.success(activiteEcritureService.modifier(
                                                idActivite, request)));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/{idActivite}/formulaire")
        public ResponseEntity<ApiResponse<ActiviteFormulaireDTO>> formulaire(
                        @PathVariable("idActivite") Integer idActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteEcritureService.formulaire(idActivite)));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/{idActivite}")
        public ResponseEntity<ApiResponse<ActiviteDetailDTO>> detail(
                        @PathVariable("idActivite") Integer idActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteDetailService.detail(idActivite)));
        }

        @RequirePermission(ressource = PermissionResource.SOUS_ACTIVITE, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/{idActivite}/sous-activites/{idSousActivite}")
        public ResponseEntity<ApiResponse<SousActiviteDetailDTO>> detailSousActivite(
                        @PathVariable("idActivite") Integer idActivite,
                        @PathVariable("idSousActivite") Integer idSousActivite) {
                return ResponseEntity.ok(
                                ApiResponse.success(activiteDetailService.detailSousActivite(
                                                idActivite, idSousActivite)));
        }

        @RequirePermission(ressource = PermissionResource.FICHIER, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.LIVRABLE, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.SOUS_ACTIVITE, action = PermissionAction.CONSULTER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
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
                                                FileNames.dispositionInline(fichier.nomOriginal()))
                                .body(fichier.contenu());
        }

        @RequirePermission(ressource = PermissionResource.LIVRABLE, action = PermissionAction.CREER)
        @RequirePermission(ressource = PermissionResource.SOUS_ACTIVITE, action = PermissionAction.MODIFIER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.MODIFIER)
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

        @RequirePermission(ressource = PermissionResource.FICHIER, action = PermissionAction.CREER)
        @RequirePermission(ressource = PermissionResource.LIVRABLE, action = PermissionAction.MODIFIER)
        @RequirePermission(ressource = PermissionResource.SOUS_ACTIVITE, action = PermissionAction.MODIFIER)
        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.MODIFIER)
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

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
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

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.CONSULTER)
        @GetMapping("/suivi/statistiques")
        public ResponseEntity<ApiResponse<ActiviteSuiviStatistiquesDTO>> statistiquesSuivi(
                        @RequestParam(required = false) Boolean pta,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) Integer serviceId,
                        @RequestParam(required = false) Integer prioriteId,
                        @RequestParam(required = false) String statut,
                        @RequestParam(required = false) Integer annee,
                        @RequestParam(required = false) Integer trimestre,
                        @RequestParam(required = false) LocalDate dateDebut,
                        @RequestParam(required = false) LocalDate dateFin) {

                ActiviteFiltreCriteria filtres = filtres(
                                pta, search, null, null, serviceId,
                                prioriteId, null, null, statut, annee, trimestre,
                                dateDebut, dateFin);

                return ResponseEntity.ok(ApiResponse.success(
                                activiteService.statistiquesSuivi(filtres)));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.MODIFIER)
        @PatchMapping("/{idActivite}/priorite")
        public ResponseEntity<ApiResponse<Void>> changerPriorite(
                        @PathVariable Integer idActivite,
                        @RequestBody ChangerPrioriteRequest request) {
                activiteService.changerPriorite(idActivite, request.getIdPriorite());
                return ResponseEntity.ok(ApiResponse.success(null));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.MODIFIER)
        @PostMapping("/{idActivite}/demarrer")
        public ResponseEntity<ApiResponse<ActiviteTransitionResponse>> demarrer(
                        @PathVariable("idActivite") Integer idActivite,
                        @RequestParam(required = false) String commentaire) {

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                activiteWorkflowService.demarrer(idActivite, commentaire)));
        }

        @RequirePermission(ressource = PermissionResource.ACTIVITE, action = PermissionAction.MODIFIER)
        @PostMapping("/{idActivite}/cloturer")
        public ResponseEntity<ApiResponse<ActiviteTransitionResponse>> cloturer(
                        @PathVariable("idActivite") Integer idActivite,
                        @RequestParam(required = false) String commentaire) {

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                activiteWorkflowService.cloturer(idActivite, commentaire)));
        }

}
