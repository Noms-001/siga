package mg.bank.backend.service.imports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.importactivite.ActiviteImportDTO;
import mg.bank.backend.dto.importactivite.ActiviteImportRequest;
import mg.bank.backend.dto.importactivite.IndicateurImportReferenceDTO;
import mg.bank.backend.dto.importactivite.LivrableImportDTO;
import mg.bank.backend.dto.importactivite.ObjectifSpecifiqueImportReferenceDTO;
import mg.bank.backend.dto.importactivite.OrigineImportReferenceDTO;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.dto.importactivite.ResultatIntermediaireImportDTO;
import mg.bank.backend.dto.importactivite.SousActiviteImportDTO;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.enums.TypeImport;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.activite.Livrable;
import mg.bank.backend.model.activite.ResultatIntermediaire;
import mg.bank.backend.model.activite.SousActivite;
import mg.bank.backend.model.indicateur.Indicateur;
import mg.bank.backend.model.origine.Origine;
import mg.bank.backend.model.referentiel.ObjectifSpecifique;
import mg.bank.backend.model.referentiel.Priorite;
import mg.bank.backend.model.referentiel.Site;
import mg.bank.backend.model.referentiel.TypeActivite;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.model.validation.Procedure;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.activite.LivrableRepository;
import mg.bank.backend.repository.activite.ResultatIntermediaireRepository;
import mg.bank.backend.repository.activite.SousActiviteRepository;
import mg.bank.backend.repository.indicateur.IndicateurRepository;
import mg.bank.backend.repository.origine.OrigineRepository;
import mg.bank.backend.repository.referentiel.ObjectifSpecifiqueRepository;
import mg.bank.backend.repository.referentiel.PrioriteRepository;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.repository.referentiel.SiteRepository;
import mg.bank.backend.repository.referentiel.TypeActiviteRepository;
import mg.bank.backend.repository.validation.ProcedureRepository;

import mg.bank.backend.service.activite.ActiviteService;


@Service
@RequiredArgsConstructor
public class ActiviteImportService {

    private static final long CLE_VERROU_IMPORT = 92_500_001L;

    /** Type d'origine attribue aux origines creees a l'import. */
    private static final String TYPE_ORIGINE_IMPORT = "IMPORT";

    /** Commentaire de la premiere entree d'historique. */
    private static final String COMMENTAIRE_HISTORIQUE_INITIAL = "Import initial";

    private final ActiviteRepository activiteRepository;
    private final SousActiviteRepository sousActiviteRepository;
    private final LivrableRepository livrableRepository;
    private final ResultatIntermediaireRepository resultatIntermediaireRepository;
    private final ServiceRepository serviceRepository;
    private final ProcedureRepository procedureRepository;
    private final PrioriteRepository prioriteRepository;
    private final TypeActiviteRepository typeActiviteRepository;
    private final SiteRepository siteRepository;
    private final ObjectifSpecifiqueRepository objectifSpecifiqueRepository;
    private final IndicateurRepository indicateurRepository;
    private final OrigineRepository origineRepository;

    private final ActiviteService activiteService;
    private final ActiviteImportValidator validator;

    // ------------------------------------------------------------------
    // Point d'entree
    // ------------------------------------------------------------------

    @Transactional
    public ResultatImportDTO importer(ActiviteImportRequest request) {

        // 1. Revalidation en profondeur (cf. §8 du cahier des charges).
        RapportValidationDTO rapport = validator.valider(request);
        if (!rapport.isValide()) {
            throw new ApiException(
                    "Import refusé : " + rapport.getNombreErreurs() + " erreur(s) détectée(s).",
                    HttpStatus.BAD_REQUEST);
        }

        // 2. Verrou consultatif : serialise les imports pour eviter qu'un code
        // genere collisionne avec celui d'un import concurrent. Le verrou
        // est relache automatiquement au commit/rollback.
        activiteRepository.verrouillerImport(CLE_VERROU_IMPORT);

        // 3. Resolution des references racine (deja validees, donc presentes).
        mg.bank.backend.model.referentiel.Service service = resoudreService(request.getService());
        Procedure procedure = resoudreProcedure(
                request.getProcedure(),
                Boolean.TRUE.equals(request.getActivitesDejaValidees()));
        String codeStatutInitial = Boolean.TRUE.equals(request.getActivitesDejaValidees())
                ? StatutsActivite.NON_COMMENCEE
                : StatutsActivite.BROUILLON;
        Utilisateur utilisateur = activiteService.utilisateurCourant();

        Map<String, ObjectifSpecifique> objectifsCache = new HashMap<>();
        Map<String, Indicateur> indicateursCache = new HashMap<>();
        Map<String, Origine> originesCache = new HashMap<>();

        List<String> codesGeneres = new ArrayList<>();
        List<String> indicateursCrees = new ArrayList<>();
        List<String> originesCreees = new ArrayList<>();

        int nbActivites = 0;
        int nbSousActivites = 0;
        int nbLivrables = 0;
        int nbResultats = 0;

        // 4. Insertion, activite par activite.
        for (ActiviteImportDTO dto : request.getActivites()) {

            // 4.a - Construction de l'activite (code genere si absent).
            String codeActivite = dto.getCode() != null && !dto.getCode().isBlank()
                    ? dto.getCode().trim()
                    : genererCodeActivite(dto, request.getTypeImport());
            if (dto.getCode() == null || dto.getCode().isBlank()) {
                codesGeneres.add(codeActivite);
            }

            // Le validator a deja verifie, mais on garde un check defensif minimal.
            if (dto.getDateDebut() != null
                    && request.getAnnee() != null
                    && dto.getDateDebut().getYear() != request.getAnnee()) {
                throw new ApiException(
                        "La date de début de l'activité « " + dto.getDesignation()
                                + " » (" + dto.getDateDebut()
                                + ") n'appartient pas à l'année de l'import ("
                                + request.getAnnee() + ").",
                        HttpStatus.BAD_REQUEST);
            }

            Activite activite = Activite.builder()
                    .code(codeActivite)
                    .reference(dto.getReference())
                    .designation(dto.getDesignation())
                    .dateDebutPrevue(dto.getDateDebut())
                    .dateFinPrevue(dto.getDateFin())
                    .dateDebutReelle(dto.getDateDebutReelle())
                    .dateFinReelle(dto.getDateFinReelle())
                    .service(service)
                    .priorite(resoudrePriorite(dto.getPriorite()))
                    .typeActivite(resoudreTypeActivite(dto.getTypeActivite()))
                    .site(resoudreSite(dto.getSite()))
                    .objectifSpecifique(resoudreObjectifSpecifique(
                            dto.getObjectifSpecifique(),
                            request.getAnnee(),
                            objectifsCache))
                    .procedure(Boolean.TRUE.equals(request.getActivitesDejaValidees())
                            ? null
                            : procedure)
                    .build();

            activite = activiteRepository.saveAndFlush(activite);
            nbActivites++;

            // 4.b - Historique initial via le chemin commun.
            activiteRepository.ajouterChangementStatut(
                    activite.getIdActivite(),
                    utilisateur.getIdUtilisateur(),
                    codeStatutInitial,
                    COMMENTAIRE_HISTORIQUE_INITIAL);

            // 4.c - Sous-activites. Le rang est global (compterActivitesJusqua)
            // pour eviter deux codes identiques entre activites.
            int rangActivite = activiteRepository.compterActivitesJusqua(activite.getIdActivite());
            int annee = activite.getDateDebutPrevue().getYear();
            int sequenceSous = 1;

            if (dto.getSousActivites() != null) {
                for (SousActiviteImportDTO saDto : dto.getSousActivites()) {

                    // Les dates reelles relevent du suivi : elles ne sont
                    // acceptees que pour des activites deja validees.
                    if (Boolean.FALSE.equals(request.getActivitesDejaValidees())
                            && (saDto.getDateDebutReelle() != null
                                    || saDto.getDateFinReelle() != null)) {
                        throw new ApiException(
                                "Les dates réelles de la sous-activité « "
                                        + saDto.getDesignation()
                                        + " » ne peuvent être fournies que pour des "
                                        + "activités déjà validées.",
                                HttpStatus.BAD_REQUEST);
                    }

                    String codeSous = saDto.getCode() != null && !saDto.getCode().isBlank()
                            ? saDto.getCode().trim()
                            : genererCodeSousActivite(annee, rangActivite, sequenceSous++);
                    if (saDto.getCode() == null || saDto.getCode().isBlank()) {
                        codesGeneres.add(codeSous);
                    }

                    SousActivite sa = sousActiviteRepository.save(SousActivite.builder()
                            .code(codeSous)
                            .designation(saDto.getDesignation())
                            .dateDebutPrevue(saDto.getDateDebut())
                            .dateFinPrevue(saDto.getDateFin())
                            .dateDebutReelle(saDto.getDateDebutReelle())
                            .dateFinReelle(saDto.getDateFinReelle())
                            .activite(activite)
                            .build());
                    nbSousActivites++;

                    if (saDto.getLivrables() != null) {
                        for (LivrableImportDTO lDto : saDto.getLivrables()) {
                            // Meme entite que le formulaire : Livrable.
                            livrableRepository.save(Livrable.builder()
                                    .designation(lDto.getDesignation())
                                    .description(lDto.getDescription())
                                    .sousActivite(sa)
                                    .build());
                            nbLivrables++;
                        }
                    }
                }
            }

            // 4.d - Resultats intermediaires (rattaches a l'activite).
            if (dto.getResultatsIntermediaires() != null) {
                for (ResultatIntermediaireImportDTO rDto : dto.getResultatsIntermediaires()) {
                    resultatIntermediaireRepository.save(ResultatIntermediaire.builder()
                            .designation(rDto.getDesignation())
                            .activite(activite)
                            .build());
                    nbResultats++;
                }
            }

            // 4.e - Indicateurs : reutilises ou crees.
            if (dto.getIndicateurs() != null) {
                for (IndicateurImportReferenceDTO iDto : dto.getIndicateurs()) {
                    Indicateur ind = resoudreOuCreerIndicateur(
                            iDto.getDesignation(),
                            indicateursCrees,
                            indicateursCache);
                    activiteRepository.lierIndicateur(activite.getIdActivite(), ind.getIdIndicateur());
                }
            }

            // 4.f - Origines : reutilisees ou creees.
            if (dto.getOrigines() != null) {
                for (OrigineImportReferenceDTO oDto : dto.getOrigines()) {
                    Origine o = resoudreOuCreerOrigine(
                            oDto.getDesignation(),
                            utilisateur,
                            originesCreees,
                            originesCache);
                    activiteRepository.lierOrigine(activite.getIdActivite(), o.getIdOrigine());
                }
            }
        }

        // 5. Resultat.
        return ResultatImportDTO.builder()
                .message("Import effectué avec succès.")
                .nombreActivitesImportees(nbActivites)
                .nombreSousActivitesImportees(nbSousActivites)
                .nombreLivrablesImportes(nbLivrables)
                .nombreResultatsIntermediairesImportes(nbResultats)
                .codesGeneres(codesGeneres)
                .indicateursCrees(indicateursCrees)
                .originesCreees(originesCreees)
                .statutInitial(codeStatutInitial)
                .build();
    }

    // ------------------------------------------------------------------
    // Generation de code
    // ------------------------------------------------------------------
    private String genererCodeActivite(ActiviteImportDTO dto, TypeImport typeImport) {
        if (TypeImport.PTA == typeImport) {
            throw new ApiException(
                    "Aucune règle de génération de code n'est définie pour les activités PTA. "
                            + "Le code doit être fourni explicitement.",
                    HttpStatus.BAD_REQUEST);
        }
        if (dto.getDateDebut() == null) {
            throw new ApiException(
                    "La date de début prévue est nécessaire pour générer le code de l'activité.",
                    HttpStatus.BAD_REQUEST);
        }
        return activiteService.prochainCodeNonPta(dto.getDateDebut().getYear());
    }

    private String genererCodeSousActivite(int annee, int rang, int sequence) {
        return String.format(Locale.ROOT, "SA-%d-%02d-%02d", annee, rang, sequence);
    }

    private String genererCodeIndicateur(int offset) {
        long base = indicateurRepository.count();
        int n = (int) base + 1 + offset;
        String code = "IND-" + n;
        while (indicateurRepository.existsByCodeIgnoreCase(code)) {
            n++;
            code = "IND-" + n;
        }
        return code;
    }

    /** Idem pour les origines. */
    private String genererCodeOrigine(int offset) {
        long base = origineRepository.count();
        int n = (int) base + 1 + offset;
        String code = "ORI-" + n;
        while (origineRepository.existsByCodeIgnoreCase(code)) {
            n++;
            code = "ORI-" + n;
        }
        return code;
    }

    // ------------------------------------------------------------------
    // Resolution des references racine
    // ------------------------------------------------------------------

    private mg.bank.backend.model.referentiel.Service resoudreService(Integer idService) {
        if (idService == null) {
            throw new ApiException(
                    "Le service est obligatoire pour l'import.",
                    HttpStatus.BAD_REQUEST);
        }
        mg.bank.backend.model.referentiel.Service service = serviceRepository.findById(idService)
                .orElseThrow(() -> new ApiException(
                        "Service introuvable (id=" + idService + ").",
                        HttpStatus.BAD_REQUEST));
        if (!Boolean.TRUE.equals(service.getActif())) {
            throw new ApiException(
                    "Le service sélectionné n'est pas actif.",
                    HttpStatus.BAD_REQUEST);
        }
        return service;
    }

    private Procedure resoudreProcedure(Integer idProcedure, boolean activitesDejaValidees) {
        if (activitesDejaValidees) {
            return null;
        }
        if (idProcedure == null) {
            throw new ApiException(
                    "La procédure est obligatoire pour des activités non encore validées.",
                    HttpStatus.BAD_REQUEST);
        }
        return procedureRepository.findById(idProcedure)
                .orElseThrow(() -> new ApiException(
                        "Procédure introuvable (id=" + idProcedure + ").",
                        HttpStatus.BAD_REQUEST));
    }

    // ------------------------------------------------------------------
    // Resolution des referentiels par libelle / code
    // ------------------------------------------------------------------

    private Priorite resoudrePriorite(String code) {
        if (code == null || code.isBlank()) {
            throw new ApiException(
                    "La priorité est obligatoire.",
                    HttpStatus.BAD_REQUEST);
        }
        Priorite priorite = prioriteRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ApiException(
                        "Priorité introuvable : « " + code + " ».",
                        HttpStatus.BAD_REQUEST));
        if (!Boolean.TRUE.equals(priorite.getActif())) {
            throw new ApiException(
                    "La priorité « " + code + " » n'est pas active.",
                    HttpStatus.BAD_REQUEST);
        }
        return priorite;
    }

    private TypeActivite resoudreTypeActivite(String designation) {
        if (designation == null || designation.isBlank()) {
            return null;
        }
        TypeActivite type = typeActiviteRepository.findByDesignationIgnoreCase(designation.trim())
                .orElseThrow(() -> new ApiException(
                        "Type d'activité introuvable : « " + designation + " ».",
                        HttpStatus.BAD_REQUEST));
        if (!Boolean.TRUE.equals(type.getActif())) {
            throw new ApiException(
                    "Le type d'activité « " + designation + " » n'est pas actif.",
                    HttpStatus.BAD_REQUEST);
        }
        return type;
    }

    private Site resoudreSite(String nom) {
        if (nom == null || nom.isBlank()) {
            return null;
        }
        Site site = siteRepository.findByNomIgnoreCase(nom.trim())
                .orElseThrow(() -> new ApiException(
                        "Site introuvable : « " + nom + " ».",
                        HttpStatus.BAD_REQUEST));
        if (!Boolean.TRUE.equals(site.getActif())) {
            throw new ApiException(
                    "Le site « " + nom + " » n'est pas actif.",
                    HttpStatus.BAD_REQUEST);
        }
        return site;
    }

    private ObjectifSpecifique resoudreObjectifSpecifique(
            ObjectifSpecifiqueImportReferenceDTO dto,
            Integer annee,
            Map<String, ObjectifSpecifique> cache) {

        if (dto == null || dto.getCode() == null || dto.getCode().isBlank()) {
            return null;
        }

        String code = dto.getCode().trim();
        String cleCache = code.toLowerCase(Locale.ROOT);

        // 0. Deja resolu dans CET import ?
        ObjectifSpecifique dejaVu = cache.get(cleCache);
        if (dejaVu != null) {
            return dejaVu;
        }

        // 1. Deja en base ?
        Optional<ObjectifSpecifique> existant = objectifSpecifiqueRepository.findByCodeIgnoreCase(code);
        if (existant.isPresent()) {
            cache.put(cleCache, existant.get());
            return existant.get();
        }

        // 2. Sinon, creation. Le validator s'est deja assure que designation
        // et annee sont fournies ; on garde un check defensif.
        if (dto.getDesignation() == null || dto.getDesignation().isBlank()) {
            throw new ApiException(
                    "La désignation de l'objectif spécifique « " + code
                            + " » est obligatoire pour le créer.",
                    HttpStatus.BAD_REQUEST);
        }
        if (annee == null) {
            throw new ApiException(
                    "L'année de l'import est nécessaire pour créer "
                            + "l'objectif spécifique « " + code + " ».",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            ObjectifSpecifique cree = objectifSpecifiqueRepository.saveAndFlush(
                    ObjectifSpecifique.builder()
                            .code(code)
                            .designation(dto.getDesignation().trim())
                            .annee(annee)
                            .build());
            cache.put(cleCache, cree);
            return cree;
        } catch (DataIntegrityViolationException e) {
            // Course avec un autre chemin : l'objectif vient d'etre cree.
            ObjectifSpecifique recupere = objectifSpecifiqueRepository
                    .findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new ApiException(
                            "Impossible de créer l'objectif spécifique « " + code + " ».",
                            HttpStatus.INTERNAL_SERVER_ERROR));
            cache.put(cleCache, recupere);
            return recupere;
        }
    }

    // ------------------------------------------------------------------
    // Indicateurs et origines : reutilisation ou creation
    // ------------------------------------------------------------------

    private Indicateur resoudreOuCreerIndicateur(
            String designation,
            List<String> crees,
            Map<String, Indicateur> cache) {

        String d = designation.trim();
        String cleCache = d.toLowerCase(Locale.ROOT);

        // 0. Deja resolu dans CET import ?
        Indicateur dejaVu = cache.get(cleCache);
        if (dejaVu != null) {
            return dejaVu;
        }

        // 1. Deja en base ?
        Optional<Indicateur> existant = indicateurRepository.findByDesignationIgnoreCase(d);
        if (existant.isPresent()) {
            cache.put(cleCache, existant.get());
            return existant.get();
        }

        // 2. Sinon, creation.
        try {
            String code = genererCodeIndicateur(crees.size());
            Indicateur cree = indicateurRepository.saveAndFlush(Indicateur.builder()
                    .code(code)
                    .designation(d)
                    .actif(Boolean.TRUE)
                    .build());
            crees.add(code);
            cache.put(cleCache, cree);
            return cree;
        } catch (DataIntegrityViolationException e) {
            Indicateur recupere = indicateurRepository.findByDesignationIgnoreCase(d)
                    .orElseThrow(() -> new ApiException(
                            "Impossible de créer l'indicateur « " + d + " ».",
                            HttpStatus.INTERNAL_SERVER_ERROR));
            cache.put(cleCache, recupere);
            return recupere;
        }
    }

    /**
     * Resout une origine par sa designation, en la creant si elle est absente.
     *
     * <p>
     * <b>Cache local</b> : meme raison que pour les indicateurs.
     * </p>
     */
    private Origine resoudreOuCreerOrigine(
            String designation,
            Utilisateur utilisateur,
            List<String> crees,
            Map<String, Origine> cache) {

        String d = designation.trim();
        String cleCache = d.toLowerCase(Locale.ROOT);

        // 0. Deja resolu dans CET import ?
        Origine dejaVu = cache.get(cleCache);
        if (dejaVu != null) {
            return dejaVu;
        }

        // 1. Deja en base ?
        Optional<Origine> existant = origineRepository.findByDesignationIgnoreCase(d);
        if (existant.isPresent()) {
            cache.put(cleCache, existant.get());
            return existant.get();
        }

        // 2. Sinon, creation.
        try {
            String code = genererCodeOrigine(crees.size());
            Origine cree = origineRepository.saveAndFlush(Origine.builder()
                    .code(code)
                    .designation(d)
                    .typeOrigine(TYPE_ORIGINE_IMPORT)
                    .utilisateur(utilisateur)
                    .build());
            crees.add(code);
            cache.put(cleCache, cree);
            return cree;
        } catch (DataIntegrityViolationException e) {
            Origine recupere = origineRepository.findByDesignationIgnoreCase(d)
                    .orElseThrow(() -> new ApiException(
                            "Impossible de créer l'origine « " + d + " ».",
                            HttpStatus.INTERNAL_SERVER_ERROR));
            cache.put(cleCache, recupere);
            return recupere;
        }
    }
}