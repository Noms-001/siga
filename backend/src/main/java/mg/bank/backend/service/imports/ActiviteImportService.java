package mg.bank.backend.service.imports;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import mg.bank.backend.service.activite.CodeActiviteService;

@Service
@RequiredArgsConstructor
public class ActiviteImportService {

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
    private final CodeActiviteService codeActiviteService;
    private final ActiviteImportValidator validator;

    public Activite chargerActivitePourCible(Integer idActivite) {
        if (idActivite == null) {
            throw new ApiException("L'activité cible est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        return activiteRepository.findById(idActivite)
                .orElseThrow(() -> new ApiException(
                        "Activité introuvable (id=" + idActivite + ").",
                        HttpStatus.NOT_FOUND));
    }

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

        // Le même verrou est utilisé par la création manuelle et les deux imports.
        codeActiviteService.verrouillerGeneration();

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
        Map<String, TypeActivite> typeActiviteCache = new HashMap<>();
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

            ObjectifSpecifique objectif = resoudreObjectifSpecifique(
                    dto.getObjectifSpecifique(),
                    request.getAnnee(),
                    objectifsCache);

                // Dates de l'activité dérivées de ses sous-activités avant de produire le code.
            List<SousActiviteImportDTO> sousDtos = dto.getSousActivites();

            LocalDate dateDebutActivite = null;
            LocalDate dateFinActivite = null;

            if (sousDtos != null && !sousDtos.isEmpty()) {
                dateDebutActivite = sousDtos.stream()
                        .map(SousActiviteImportDTO::getDateDebut)
                        .filter(Objects::nonNull)
                        .min(LocalDate::compareTo)
                        .orElseThrow(() -> new ApiException(
                                "La date de début de l'activité « " + dto.getDesignation()
                                        + " » ne peut pas être dérivée : aucune sous-activité "
                                        + "n'a de date de début.",
                                HttpStatus.BAD_REQUEST));

                dateFinActivite = sousDtos.stream()
                        .map(SousActiviteImportDTO::getDateFin)
                        .filter(Objects::nonNull)
                        .max(LocalDate::compareTo)
                        .orElse(dateDebutActivite);

                if (request.getAnnee() != null
                        && dateDebutActivite.getYear() != request.getAnnee()) {
                    throw new ApiException(
                            "La date de début dérivée de l'activité « " + dto.getDesignation()
                                    + " » (" + dateDebutActivite + ") n'appartient pas à l'année "
                                    + "de l'import (" + request.getAnnee() + ").",
                            HttpStatus.BAD_REQUEST);
                }
            }

                boolean codeActiviteAbsent = dto.getCode() == null || dto.getCode().isBlank();
                String codeActivite = codeActiviteAbsent
                    ? codeActiviteService.genererCodeActivite(
                        request.getTypeImport(), objectif, request.getAnnee(), service.getIdService())
                    : codeActiviteService.validerCodeActiviteFourni(
                        dto.getCode(), request.getTypeImport(), objectif,
                        request.getAnnee(), service.getIdService());
                if (codeActiviteAbsent) {
                codesGeneres.add(codeActivite);
                }

                // La référence PTA reste dérivée du code attribué ou fourni.
                String referenceActivite = (TypeImport.PTA == request.getTypeImport())
                    ? genererReferenceActivite(objectif, codeActivite)
                    : null;

            Activite activite = Activite.builder()
                    .code(codeActivite)
                    .reference(referenceActivite)
                    .designation(dto.getDesignation())
                    .dateDebutPrevue(dateDebutActivite)
                    .dateFinPrevue(dateFinActivite)
                    .service(service)
                    .priorite(resoudrePriorite(dto.getPriorite()))
                    .typeActivite(resoudreTypeActivite(dto.getTypeActivite(), typeActiviteCache))
                    .site(resoudreSite(dto.getSite()))
                    .objectifSpecifique(objectif)
                    .procedure(Boolean.TRUE.equals(request.getActivitesDejaValidees()) ? null : procedure)
                    .build();

            activite = activiteRepository.saveAndFlush(activite);
            nbActivites++;

            activiteRepository.ajouterChangementStatut(
                    activite.getIdActivite(),
                    utilisateur.getIdUtilisateur(),
                    codeStatutInitial,
                    COMMENTAIRE_HISTORIQUE_INITIAL);

            if (sousDtos != null && !sousDtos.isEmpty()) {
                for (SousActiviteImportDTO saDto : sousDtos) {

                    boolean codeSousAbsent = saDto.getCode() == null || saDto.getCode().isBlank();
                    String codeSous = codeSousAbsent
                        ? codeActiviteService.genererCodeSousActivite(activite)
                        : codeActiviteService.validerCodeSousActiviteFourni(saDto.getCode(), activite);
                    if (codeSousAbsent) {
                    codesGeneres.add(codeSous);
                    }

                    SousActivite sa = sousActiviteRepository.save(SousActivite.builder()
                            .code(codeSous)
                            .designation(saDto.getDesignation())
                            .dateDebutPrevue(saDto.getDateDebut())
                            .dateFinPrevue(saDto.getDateFin())
                            // dates reelles restent null : relevent du suivi
                            .activite(activite)
                            .build());
                    nbSousActivites++;

                    if (saDto.getLivrables() != null) {
                        for (LivrableImportDTO lDto : saDto.getLivrables()) {
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

            // 4.d - Resultats intermediaires
            if (dto.getResultatsIntermediaires() != null) {
                for (ResultatIntermediaireImportDTO rDto : dto.getResultatsIntermediaires()) {
                    resultatIntermediaireRepository.save(ResultatIntermediaire.builder()
                            .designation(rDto.getDesignation())
                            .activite(activite)
                            .build());
                    nbResultats++;
                }
            }

            // 4.e - Indicateurs
            if (dto.getIndicateurs() != null) {
                for (IndicateurImportReferenceDTO iDto : dto.getIndicateurs()) {
                    Indicateur ind = resoudreOuCreerIndicateur(iDto, indicateursCrees, indicateursCache);
                    activiteRepository.lierIndicateur(activite.getIdActivite(), ind.getIdIndicateur());
                }
            }

            // 4.f - Origines
            if (dto.getOrigines() != null) {
                for (OrigineImportReferenceDTO oDto : dto.getOrigines()) {
                    Origine o = resoudreOuCreerOrigine(
                            oDto.getDesignation(), utilisateur, originesCreees, originesCache);
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
    /**
     * Genere la reference PTA : {@code <codeObjectif>_<numActivite>}, le
     * numero etant extrait du code genere.
     */
    private String genererReferenceActivite(ObjectifSpecifique objectif, String codeActivite) {
        if (objectif == null) {
            throw new ApiException(
                    "Un objectif spécifique est requis pour générer la référence d'une activité PTA.",
                    HttpStatus.BAD_REQUEST);
        }
        int numeroActivite = extraireNumeroActivite(codeActivite);
        return objectif.getCode().trim() + "_" + numeroActivite;
    }

    private int extraireNumeroActivite(String codeActivite) {
        if (codeActivite == null || codeActivite.isBlank()) {
            throw new ApiException(
                    "Le code de l'activité est requis pour générer sa référence PTA.",
                    HttpStatus.BAD_REQUEST);
        }
        String[] segments = codeActivite.split("[-/]");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i] != null && segments[i].matches("\\d+")) {
                return Integer.parseInt(segments[i]);
            }
        }
        throw new ApiException(
                "Impossible de dériver le numéro d'activité depuis le code « " + codeActivite + ".",
                HttpStatus.BAD_REQUEST);
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
            return prioriteRepository.findByCodeIgnoreCase("NORMALE")
                    .filter(Priorite::getActif)
                    .orElseThrow(() -> new ApiException(
                            "La priorité par défaut « NORMALE » est absente ou inactive.",
                            HttpStatus.INTERNAL_SERVER_ERROR));
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

    private TypeActivite resoudreTypeActivite(String designation, Map<String, TypeActivite> cache) {
        if (designation == null || designation.isBlank()) {
            return null;
        }

        String cle = designation.trim();
        String cleCache = cle.toLowerCase(Locale.ROOT);
        TypeActivite dejaVu = cache.get(cleCache);
        if (dejaVu != null) {
            return dejaVu;
        }

        TypeActivite type = typeActiviteRepository.findByDesignationIgnoreCase(cle)
                .orElseGet(() -> {
                    TypeActivite cree = typeActiviteRepository.saveAndFlush(TypeActivite.builder()
                            .designation(cle)
                            .description("Type créé automatiquement lors d'un import")
                            .actif(Boolean.TRUE)
                            .dateCreation(LocalDateTime.now())
                            .build());
                    cache.put(cleCache, cree);
                    return cree;
                });

        if (!Boolean.TRUE.equals(type.getActif())) {
            throw new ApiException(
                    "Le type d'activité « " + designation + " » n'est pas actif.",
                    HttpStatus.BAD_REQUEST);
        }
        cache.put(cleCache, type);
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

    // --- Signature modifiée ---
    Indicateur resoudreOuCreerIndicateur(
            IndicateurImportReferenceDTO dto,
            List<String> crees,
            Map<String, Indicateur> cache) {

        String d = dto.getDesignation().trim();
        String cleCache = d.toLowerCase(Locale.ROOT);

        Indicateur dejaVu = cache.get(cleCache);
        if (dejaVu != null) {
            return dejaVu;
        }

        Optional<Indicateur> existant = indicateurRepository.findByDesignationIgnoreCase(d);
        if (existant.isPresent()) {
            cache.put(cleCache, existant.get());
            return existant.get();
        }

        try {
            String code = genererCodeIndicateur(crees.size());
            Indicateur cree = indicateurRepository.saveAndFlush(Indicateur.builder()
                    .code(code)
                    .designation(d)
                    .typeIndicateur(emptyToNull(dto.getTypeIndicateur()))
                    .uniteMesure(emptyToNull(dto.getUniteMesure()))
                    .frequenceVerification(emptyToNull(dto.getFrequenceVerification()))
                    .frequenceAggregation(emptyToNull(dto.getFrequenceAggregation()))
                    .definition(emptyToNull(dto.getDefinition()))
                    .methodeDetermination(emptyToNull(dto.getMethodeDetermination()))
                    .objectif(emptyToNull(dto.getObjectif()))
                    .valeurCible(dto.getValeurCible())
                    .seuilMin(dto.getSeuilMin())
                    .seuilMax(dto.getSeuilMax())
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

    // --- Helper ajouté ---
    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
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