package mg.bank.backend.service.activite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.ActiviteEcritureRequest;
import mg.bank.backend.dto.activite.ActiviteEcritureResponse;
import mg.bank.backend.dto.activite.ActiviteFormulaireDTO;
import mg.bank.backend.dto.activite.LivrableEcritureRequest;
import mg.bank.backend.dto.activite.LivrableFormulaireDTO;
import mg.bank.backend.dto.activite.ResultatIntermediaireEcritureRequest;
import mg.bank.backend.dto.activite.ResultatIntermediaireFormulaireDTO;
import mg.bank.backend.dto.activite.SousActiviteEcritureRequest;
import mg.bank.backend.dto.activite.SousActiviteFormulaireDTO;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.activite.Livrable;
import mg.bank.backend.model.activite.ResultatIntermediaire;
import mg.bank.backend.model.activite.SousActivite;
import mg.bank.backend.model.parametre.Parametre;
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
import mg.bank.backend.repository.parametre.ParametreRepository;
import mg.bank.backend.repository.projection.common.StatutCourantRow;
import mg.bank.backend.repository.referentiel.ObjectifSpecifiqueRepository;
import mg.bank.backend.repository.referentiel.PrioriteRepository;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.repository.referentiel.SiteRepository;
import mg.bank.backend.repository.referentiel.TypeActiviteRepository;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@Service
@RequiredArgsConstructor
public class ActiviteEcritureService {

        private final ActiviteRepository activiteRepository;

        private final SousActiviteRepository sousActiviteRepository;

        private final ResultatIntermediaireRepository resultatIntermediaireRepository;

        private final LivrableRepository livrableRepository;

        private final PrioriteRepository prioriteRepository;

        private final TypeActiviteRepository typeActiviteRepository;

        private final SiteRepository siteRepository;

        private final ObjectifSpecifiqueRepository objectifSpecifiqueRepository;

        private final ServiceRepository serviceRepository;

        private final ActiviteService activiteService;

        private final ParametreRepository parametreRepository;

        private final CurrentUserService currentUserService;

        @Transactional(readOnly = true)
        public ActiviteFormulaireDTO formulaire(Integer idActivite) {

                validerId(idActivite);

                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

                Activite activite = activiteRepository.findById(idActivite)
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

                StatutCourantRow statut = statutCourant(idActivite, perimetre)
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

                List<SousActiviteFormulaireDTO> sousActivites = sousActiviteRepository
                                .findByActiviteIdActiviteOrderByDateDebutPrevueAscCodeAsc(
                                                activite.getIdActivite())
                                .stream()
                                .map(this::versFormulaireSousActivite)
                                .toList();

                List<ResultatIntermediaireFormulaireDTO> resultatsIntermediaires = resultatIntermediaireRepository
                                .findByActiviteIdActiviteOrderByIdResultatIntermediaireAsc(
                                                activite.getIdActivite())
                                .stream()
                                .map(this::versFormulaireResultatIntermediaire)
                                .toList();

                return ActiviteFormulaireDTO.builder()
                                .idActivite(activite.getIdActivite())
                                .code(activite.getCode())
                                .reference(activite.getReference())
                                .designation(activite.getDesignation())
                                .dateDebutPrevue(activite.getDateDebutPrevue())
                                .dateFinPrevue(activite.getDateFinPrevue())
                                .idObjectifSpecifique(
                                                activite.getObjectifSpecifique() != null
                                                                ? activite.getObjectifSpecifique()
                                                                                .getIdObjectifSpecifique()
                                                                : null)
                                .objectifCode(
                                                activite.getObjectifSpecifique() != null
                                                                ? activite.getObjectifSpecifique().getCode()
                                                                : null)
                                .objectifDesignation(
                                                activite.getObjectifSpecifique() != null
                                                                ? activite.getObjectifSpecifique().getDesignation()
                                                                : null)
                                .objectifAnnee(
                                                activite.getObjectifSpecifique() != null
                                                                ? activite.getObjectifSpecifique().getAnnee()
                                                                : null)
                                .idTypeActivite(
                                                activite.getTypeActivite() != null
                                                                ? activite.getTypeActivite().getIdTypeActivite()
                                                                : null)
                                .idSite(
                                                activite.getSite() != null
                                                                ? activite.getSite().getIdSite()
                                                                : null)
                                .idPriorite(activite.getPriorite().getIdPriorite())
                                .idService(activite.getService().getIdService())
                                .statut(statut.getStatutCode())
                                .sousActivites(sousActivites)
                                .resultatsIntermediaires(resultatsIntermediaires)
                                .build();
        }

        // ------------------------------------------------------------------
        // Ecriture
        // ------------------------------------------------------------------
        @Transactional
        public ActiviteEcritureResponse creer(ActiviteEcritureRequest request) {

                Utilisateur auteur = currentUserService.get();

                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

                validerDates(request.getDateDebutPrevue(), request.getDateFinPrevue());

                ObjectifSpecifique objectif = resoudreObjectif(request);

                String reference = resoudreReference(request);
                TypeActivite typeActivite = resoudreTypeActivite(request.getIdTypeActivite());
                Optional<Parametre> id_procedure;
                if (objectif != null) {
                        id_procedure = parametreRepository.findByCodeAndActifTrue("ID_PROCEDURE_PTA");
                } else {
                        id_procedure = parametreRepository.findByCodeAndActifTrue("ID_PROCEDURE_NON_PTA");
                }

                if (activiteRepository.existsByCodeIgnoreCase(request.getCode().trim())) {
                        throw new ApiException(
                                        "Ce code d'activité est déjà utilisé",
                                        HttpStatus.BAD_REQUEST);
                }

                Activite activite = activiteRepository.saveAndFlush(Activite.builder()
                                .code(request.getCode().trim())
                                .reference(reference)
                                .designation(request.getDesignation().trim())
                                .dateDebutPrevue(request.getDateDebutPrevue())
                                .dateFinPrevue(request.getDateFinPrevue())
                                .objectifSpecifique(objectif)
                                .typeActivite(typeActivite)
                                .site(resoudreSite(request.getIdSite()))
                                .priorite(resoudrePriorite(request.getIdPriorite()))
                                .service(resoudreService(perimetre, request.getIdService()))
                                .build());

                if (id_procedure.isPresent()) {
                        int id = Integer.parseInt(id_procedure.get().getValeur());
                        activite.setProcedure(Procedure.builder().idProcedure(id).build());
                } else {
                        throw new ApiException(
                                        "Impossible de trouver la procédure pour le type d'activité",
                                        HttpStatus.INTERNAL_SERVER_ERROR);
                }

                activiteRepository.ajouterChangementStatut(
                                activite.getIdActivite(),
                                auteur.getIdUtilisateur(),
                                StatutsActivite.BROUILLON,
                                "Création de l'activité");

                List<SousActivite> sousActivites = synchroniserSousActivites(
                                activite,
                                request.getSousActivites(),
                                List.of());

                int nombreResultats = remplacerResultatsIntermediaires(
                                activite,
                                request.getResultatsIntermediaires());

                return reponse(activite, sousActivites.size(), nombreResultats);
        }

        @Transactional
        public ActiviteEcritureResponse modifier(
                        Integer idActivite,
                        ActiviteEcritureRequest request) {

                validerId(idActivite);

                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

                // Verrou pris avant toute relecture du statut.
                Activite activite = activiteRepository.verrouillerActivite(idActivite)
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

                StatutCourantRow statut = statutCourant(idActivite, perimetre)
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

                if (!StatutsActivite.BROUILLON.equals(statut.getStatutCode())) {
                        throw new ApiException(
                                        "Seule une activité en brouillon peut être modifiée",
                                        HttpStatus.CONFLICT);
                }

                validerDates(request.getDateDebutPrevue(), request.getDateFinPrevue());

                ObjectifSpecifique objectif = resoudreObjectif(request);

                String nouveauCode = request.getCode().trim();

                String reference = resoudreReference(request);

                if (!nouveauCode.equalsIgnoreCase(activite.getCode())
                                && activiteRepository.existsByCodeIgnoreCase(nouveauCode)) {
                        throw new ApiException(
                                        "Ce code d'activité est déjà utilisé",
                                        HttpStatus.BAD_REQUEST);
                }

                List<SousActivite> existantes = sousActiviteRepository
                                .findByActiviteIdActiviteOrderByDateDebutPrevueAscCodeAsc(
                                                activite.getIdActivite());

                List<SousActiviteEcritureRequest> demandees = request.getSousActivites() == null
                                ? List.of()
                                : request.getSousActivites();

                activite.setCode(nouveauCode);
                activite.setReference(reference);
                activite.setDesignation(request.getDesignation().trim());
                activite.setDateDebutPrevue(request.getDateDebutPrevue());
                activite.setDateFinPrevue(request.getDateFinPrevue());
                activite.setObjectifSpecifique(objectif);
                activite.setTypeActivite(resoudreTypeActivite(request.getIdTypeActivite()));
                activite.setSite(resoudreSite(request.getIdSite()));
                activite.setPriorite(resoudrePriorite(request.getIdPriorite()));
                activite.setService(resoudreService(perimetre, request.getIdService()));

                activiteRepository.saveAndFlush(activite);

                List<SousActivite> sousActivites = synchroniserSousActivites(
                                activite,
                                request.getSousActivites(),
                                existantes);

                int nombreResultats = remplacerResultatsIntermediaires(
                                activite,
                                request.getResultatsIntermediaires());

                return reponse(activite, sousActivites.size(), nombreResultats);
        }

        private void synchroniserLivrables(
                        SousActivite sousActivite,
                        List<LivrableEcritureRequest> demandes,
                        List<Livrable> existantes) {

                List<LivrableEcritureRequest> entrees = demandes == null ? List.of() : demandes;

                Set<String> dejaCouverts = new HashSet<>();

                Set<Livrable> aSupprimer = new HashSet<>();

                for (Livrable existante : existantes) {

                        String designation = texte(existante.getDesignation());

                        boolean encoreDemandee = entrees.stream()
                                        .anyMatch(entree -> designation
                                                        .equalsIgnoreCase(texte(entree.getDesignation())));

                        if (encoreDemandee || livrableRepository
                                        .compterFichiers(existante.getIdLivrable()) > 0) {
                                dejaCouverts.add(designation.toLowerCase());
                                continue;
                        }

                        aSupprimer.add(existante);
                }

                if (!aSupprimer.isEmpty()) {
                        livrableRepository.deleteAll(aSupprimer);
                }

                for (LivrableEcritureRequest entree : entrees) {

                        String designation = texte(entree.getDesignation());

                        if (!dejaCouverts.add(designation.toLowerCase())) {
                                continue;
                        }

                        livrableRepository.save(Livrable.builder()
                                        .designation(designation)
                                        .description(entree.getDescription() == null
                                                        || entree.getDescription().isBlank()
                                                                        ? null
                                                                        : entree.getDescription().trim())
                                        .sousActivite(sousActivite)
                                        .build());
                }
        }

        private int remplacerResultatsIntermediaires(
                        Activite activite,
                        List<ResultatIntermediaireEcritureRequest> demandes) {

                resultatIntermediaireRepository
                                .deleteAll(resultatIntermediaireRepository
                                                .findByActiviteIdActiviteOrderByIdResultatIntermediaireAsc(
                                                                activite.getIdActivite()));

                if (demandes == null || demandes.isEmpty()) {
                        return 0;
                }

                List<ResultatIntermediaire> aEnregistrer = demandes.stream()
                                .map(demande -> ResultatIntermediaire.builder()
                                                .designation(demande.getDesignation().trim())
                                                .activite(activite)
                                                .build())
                                .toList();

                resultatIntermediaireRepository.saveAll(aEnregistrer);

                return aEnregistrer.size();
        }

        private List<SousActivite> synchroniserSousActivites(
                        Activite activite,
                        List<SousActiviteEcritureRequest> demandees,
                        List<SousActivite> existantes) {

                List<SousActiviteEcritureRequest> entrees = demandees == null ? List.of() : demandees;

                Set<Integer> identifiantsExistants = new HashSet<>();

                Map<Integer, String> codeParLigne = new HashMap<>();

                Set<String> codesPris = new HashSet<>();

                String prefixe = entrees.stream()
                                .anyMatch(entree -> entree.getCode() == null
                                                || entree.getCode().isBlank())
                                                                ? prefixeSousActivite(activite)
                                                                : null;

                existantes.forEach(existante -> {
                        identifiantsExistants.add(existante.getIdSousActivite());
                        codeParLigne.put(existante.getIdSousActivite(), existante.getCode());
                        codesPris.add(majuscule(existante.getCode()));
                });

                List<SousActivite> synchronisees = new ArrayList<>();

                Set<Integer> conserves = new HashSet<>();

                for (SousActiviteEcritureRequest entree : entrees) {

                        if (entree.getIdSousActivite() != null
                                        && !identifiantsExistants.contains(entree.getIdSousActivite())) {
                                throw new ApiException(
                                                "Cette sous-activité n'appartient pas à l'activité",
                                                HttpStatus.BAD_REQUEST);
                        }

                        // Deux lignes pour la meme sous-activite : la derniere ecraserait
                        // la premiere, silencieusement. Le refus est explicite.
                        if (entree.getIdSousActivite() != null
                                        && !conserves.add(entree.getIdSousActivite())) {
                                throw new ApiException(
                                                "Cette sous-activité apparaît plusieurs fois dans le"
                                                                + " formulaire",
                                                HttpStatus.BAD_REQUEST);
                        }

                        validerDates(entree.getDateDebutPrevue(), entree.getDateFinPrevue());

                        String code = resoudreCodeSousActivite(
                                        entree, prefixe, codeParLigne, codesPris);

                        codesPris.add(majuscule(code));

                        if (entree.getIdSousActivite() == null) {

                                SousActivite creee = sousActiviteRepository.save(SousActivite.builder()
                                                .code(code)
                                                .designation(entree.getDesignation().trim())
                                                .dateDebutPrevue(entree.getDateDebutPrevue())
                                                .dateFinPrevue(entree.getDateFinPrevue())
                                                .activite(activite)
                                                .build());

                                synchroniserLivrables(creee, entree.getLivrables(), List.of());

                                synchronisees.add(creee);

                                continue;
                        }

                        SousActivite existante = existantes.stream()
                                        .filter(enregistree -> enregistree.getIdSousActivite()
                                                        .equals(entree.getIdSousActivite()))
                                        .findFirst()
                                        .orElseThrow(() -> new ApiException(
                                                        "Cette sous-activité n'appartient pas à l'activité",
                                                        HttpStatus.BAD_REQUEST));

                        codeParLigne.put(existante.getIdSousActivite(), code);

                        conserves.add(existante.getIdSousActivite());

                        synchroniserLivrables(
                                        existante,
                                        entree.getLivrables(),
                                        livrableRepository
                                                        .findBySousActiviteIdSousActiviteOrderByIdLivrableAsc(
                                                                        existante.getIdSousActivite()));

                        if (!sousActiviteModifiee(existante, entree, code)) {
                                synchronisees.add(existante);
                                continue;
                        }

                        // Les dates reelles ne sont pas reecrites : elles appartiennent
                        // au suivi, et une redaction ne doit pas ecraser une execution.
                        existante.setCode(code);
                        existante.setDesignation(entree.getDesignation().trim());
                        existante.setDateDebutPrevue(entree.getDateDebutPrevue());
                        existante.setDateFinPrevue(entree.getDateFinPrevue());

                        synchronisees.add(sousActiviteRepository.save(existante));
                }

                for (SousActivite existante : existantes) {

                        if (conserves.contains(existante.getIdSousActivite())) {
                                continue;
                        }

                        long dependances = sousActiviteRepository.compterDependances(
                                        existante.getIdSousActivite());

                        if (dependances > 0) {
                                throw new ApiException(
                                                "La sous-activité « " + existante.getCode() + " » porte"
                                                                + " encore une affectation, un livrable ou un"
                                                                + " avancement et ne peut pas être supprimée",
                                                HttpStatus.CONFLICT);
                        }

                        sousActiviteRepository.delete(existante);
                }

                return synchronisees;
        }

        private static String majuscule(String valeur) {
                return valeur == null ? null : valeur.toUpperCase();
        }

        private static String texte(String valeur) {
                return valeur == null ? "" : valeur.trim();
        }

        private static boolean sousActiviteModifiee(
                        SousActivite existante,
                        SousActiviteEcritureRequest demandee,
                        String codeResolu) {

                if (demandee == null) {
                        return false;
                }

                boolean memeCode = codeResolu != null
                                ? codeResolu.equalsIgnoreCase(existante.getCode())
                                : Objects.equals(
                                                texte(demandee.getCode()),
                                                texte(existante.getCode()));

                return !memeCode
                                || !Objects.equals(
                                                texte(demandee.getDesignation()),
                                                texte(existante.getDesignation()))
                                || !Objects.equals(
                                                demandee.getDateDebutPrevue(), existante.getDateDebutPrevue())
                                || !Objects.equals(
                                                demandee.getDateFinPrevue(), existante.getDateFinPrevue());
        }

        private <T> T resoudreActif(
                        java.util.Optional<T> candidat,
                        java.util.function.Function<T, Boolean> estActif,
                        String messageIntrouvable,
                        String messageInactif) {

                T entity = candidat.orElseThrow(() -> new ApiException(
                                messageIntrouvable, HttpStatus.BAD_REQUEST));

                if (!Boolean.TRUE.equals(estActif.apply(entity))) {
                        throw new ApiException(messageInactif, HttpStatus.BAD_REQUEST);
                }

                return entity;
        }

        private String resoudreCodeSousActivite(
                        SousActiviteEcritureRequest entree,
                        String prefixe,
                        Map<Integer, String> codeParLigne,
                        Set<String> codesPris) {

                if (entree.getCode() != null && !entree.getCode().isBlank()) {

                        String code = entree.getCode().trim();

                        if (code.length() > 20) {
                                throw new ApiException(
                                                "Le code d'une sous-activité ne doit pas dépasser"
                                                                + " 20 caractères",
                                                HttpStatus.BAD_REQUEST);
                        }

                        if (estLibre(code, entree.getIdSousActivite(), codeParLigne, codesPris)) {
                                return code;
                        }

                        throw new ApiException(
                                        "Ce code de sous-activité est déjà utilisé",
                                        HttpStatus.BAD_REQUEST);
                }

                for (int numero = 1; numero <= 99; numero++) {

                        String candidat = prefixe + String.format("%02d", numero);

                        if (estLibre(candidat, entree.getIdSousActivite(), codeParLigne, codesPris)) {
                                return candidat;
                        }
                }

                throw new ApiException(
                                "Impossible de générer un code de sous-activité pour cette"
                                                + " activité",
                                HttpStatus.BAD_REQUEST);
        }

        private String prefixeSousActivite(Activite activite) {

                String code = activite.getCode() == null ? "" : activite.getCode().trim();

                if (activite.getObjectifSpecifique() != null
                                && code.length() > 2
                                && code.charAt(0) == 'A'
                                && code.charAt(1) == '-') {
                        return "SA-" + code.substring(2) + "-";
                }

                int rang = activiteRepository.compterActivitesJusqua(
                                activite.getIdActivite());

                return "SA-" + activite.getDateDebutPrevue().getYear()
                                + "-" + String.format("%02d", rang) + "-";
        }

        private boolean estLibre(
                        String code,
                        Integer idLigne,
                        Map<Integer, String> codeParLigne,
                        Set<String> codesPris) {

                if (codesPris.contains(majuscule(code))) {

                        String codeDeMaLigne = idLigne == null ? null : codeParLigne.get(idLigne);

                        return codeDeMaLigne != null
                                        && codeDeMaLigne.equalsIgnoreCase(code);
                }

                return !sousActiviteRepository.existsByCodeIgnoreCase(code);
        }

        // ------------------------------------------------------------------
        // Resolution des referentiels
        // ------------------------------------------------------------------
        private mg.bank.backend.model.referentiel.Service resoudreService(
                        PerimetreUtilisateur perimetre,
                        Integer idServiceRecu) {

                if (perimetre.estRattacheService()) {
                        return serviceRepository.findById(perimetre.getIdService())
                                        .orElseThrow(() -> new ApiException(
                                                        "Votre service n'existe plus",
                                                        HttpStatus.FORBIDDEN));
                }

                if (idServiceRecu == null) {
                        throw new ApiException(
                                        "Le service est obligatoire",
                                        HttpStatus.BAD_REQUEST);
                }

                mg.bank.backend.model.referentiel.Service service = serviceRepository.findById(idServiceRecu)
                                .orElseThrow(() -> new ApiException(
                                                "Le service sélectionné n'existe pas",
                                                HttpStatus.BAD_REQUEST));

                if (!Boolean.TRUE.equals(service.getActif())) {
                        throw new ApiException(
                                        "Le service sélectionné n'est pas actif",
                                        HttpStatus.BAD_REQUEST);
                }

                if (perimetre.estNiveauDepartement()
                                && !perimetre.getIdDepartement().equals(
                                                service.getDepartement().getIdDepartement())) {
                        throw new ApiException(
                                        "Le service sélectionné n'appartient pas à votre"
                                                        + " département",
                                        HttpStatus.FORBIDDEN);
                }

                return service;
        }

        private ObjectifSpecifique resoudreObjectif(ActiviteEcritureRequest request) {

                boolean pta = Boolean.TRUE.equals(request.getPta());
                Integer idObjectif = request.getIdObjectifSpecifique();

                if (pta && idObjectif == null) {
                        throw new ApiException(
                                        "Une activité PTA doit être rattachée à un objectif"
                                                        + " spécifique",
                                        HttpStatus.BAD_REQUEST);
                }

                if (!pta && idObjectif != null) {
                        throw new ApiException(
                                        "Une activité non PTA ne peut pas être rattachée à un"
                                                        + " objectif spécifique",
                                        HttpStatus.BAD_REQUEST);
                }

                if (idObjectif == null) {
                        return null;
                }

                return objectifSpecifiqueRepository.findById(idObjectif)
                                .orElseThrow(() -> new ApiException(
                                                "L'objectif spécifique sélectionné n'existe pas",
                                                HttpStatus.BAD_REQUEST));
        }

        private String resoudreReference(ActiviteEcritureRequest request) {

                String reference = request.getReference() == null
                                ? ""
                                : request.getReference().trim();

                if (Boolean.TRUE.equals(request.getPta())) {

                        if (reference.isEmpty()) {
                                throw new ApiException(
                                                "Une activité PTA doit porter une référence",
                                                HttpStatus.BAD_REQUEST);
                        }

                        return reference;
                }

                return null;
        }

        private Priorite resoudrePriorite(Integer idPriorite) {
                return resoudreActif(
                                prioriteRepository.findById(idPriorite),
                                Priorite::getActif,
                                "La priorité sélectionnée n'existe pas",
                                "La priorité sélectionnée n'est pas active");
        }

        private TypeActivite resoudreTypeActivite(Integer idTypeActivite) {
                if (idTypeActivite == null) {
                        return null;
                }
                return resoudreActif(
                                typeActiviteRepository.findById(idTypeActivite),
                                TypeActivite::getActif,
                                "Le type d'activité sélectionné n'existe pas",
                                "Le type d'activité sélectionné n'est pas actif");
        }

        private Site resoudreSite(Integer idSite) {
                if (idSite == null) {
                        return null;
                }
                return resoudreActif(
                                siteRepository.findById(idSite),
                                Site::getActif,
                                "Le site sélectionné n'existe pas",
                                "Le site sélectionné n'est pas actif");
        }

        // ------------------------------------------------------------------
        // Verifications transverses
        // ------------------------------------------------------------------
        private void validerDates(LocalDate debut, LocalDate fin) {

                if (debut == null) {
                        throw new ApiException(
                                        "La date de début prévue est obligatoire",
                                        HttpStatus.BAD_REQUEST);
                }

                if (fin != null && fin.isBefore(debut)) {
                        throw new ApiException(
                                        "La date de fin prévue ne peut pas précéder la date de"
                                                        + " début prévue",
                                        HttpStatus.BAD_REQUEST);
                }
        }

        private void validerId(Integer idActivite) {

                if (idActivite == null || idActivite < 1) {
                        throw new ApiException(
                                        "L'identifiant de l'activité doit être un entier positif",
                                        HttpStatus.BAD_REQUEST);
                }
        }

        private Optional<StatutCourantRow> statutCourant(
                        Integer idActivite,
                        PerimetreUtilisateur perimetre) {

                return activiteRepository.findStatutCourant(
                                idActivite,
                                perimetre.getFiltreService(),
                                perimetre.getFiltreDepartement());
        }

        private ActiviteEcritureResponse reponse(
                        Activite activite,
                        int nombreSousActivites,
                        int nombreResultatsIntermediaires) {
                return ActiviteEcritureResponse.builder()
                                .idActivite(activite.getIdActivite())
                                .code(activite.getCode())
                                .reference(activite.getReference())
                                .designation(activite.getDesignation())
                                .statut(StatutsActivite.BROUILLON)
                                .dateDebutPrevue(activite.getDateDebutPrevue())
                                .dateFinPrevue(activite.getDateFinPrevue())
                                .dateEnregistrement(LocalDateTime.now())
                                .nombreSousActivites(nombreSousActivites)
                                .nombreResultatsIntermediaires(nombreResultatsIntermediaires)
                                .build();
        }

        private SousActiviteFormulaireDTO versFormulaireSousActivite(SousActivite sous) {
                return SousActiviteFormulaireDTO.builder()
                                .idSousActivite(sous.getIdSousActivite())
                                .code(sous.getCode())
                                .designation(sous.getDesignation())
                                .dateDebutPrevue(sous.getDateDebutPrevue())
                                .dateFinPrevue(sous.getDateFinPrevue())
                                .dateDebutReelle(sous.getDateDebutReelle())
                                .dateFinReelle(sous.getDateFinReelle())
                                .livrables(livrableRepository
                                                .findBySousActiviteIdSousActiviteOrderByIdLivrableAsc(
                                                                sous.getIdSousActivite())
                                                .stream()
                                                .map(this::versFormulaireLivrable)
                                                .toList())
                                .build();
        }

        private LivrableFormulaireDTO versFormulaireLivrable(Livrable livrable) {
                return LivrableFormulaireDTO.builder()
                                .idLivrable(livrable.getIdLivrable())
                                .designation(livrable.getDesignation())
                                .description(livrable.getDescription())
                                .nombreFichiers(livrableRepository.compterFichiers(
                                                livrable.getIdLivrable()))
                                .build();
        }

        private ResultatIntermediaireFormulaireDTO versFormulaireResultatIntermediaire(
                        ResultatIntermediaire resultat) {
                return ResultatIntermediaireFormulaireDTO.builder()
                                .idResultatIntermediaire(resultat.getIdResultatIntermediaire())
                                .designation(resultat.getDesignation())
                                .build();
        }
}
