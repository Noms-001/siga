package mg.bank.backend.service.activite;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.ActiviteListItemDTO;
import mg.bank.backend.dto.activite.ActiviteOptionsDTO;
import mg.bank.backend.dto.activite.ActiviteStatistiquesDTO;
import mg.bank.backend.dto.activite.ActiviteSuiviDTO;
import mg.bank.backend.dto.activite.AutocompleteDTO;
import mg.bank.backend.dto.activite.ObjectifSpecifiqueEcritureRequest;
import mg.bank.backend.dto.activite.ResponsableSuiviDTO;
import mg.bank.backend.dto.activite.SousActiviteSuiviDTO;
import mg.bank.backend.dto.common.ActiviteFiltreCriteria;
import mg.bank.backend.dto.common.OptionDTO;
import mg.bank.backend.dto.common.PageResponse;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.enums.DecisionValidation;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.activite.ActiviteMapper;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.activite.HistoriqueActivite;
import mg.bank.backend.model.referentiel.ObjectifSpecifique;
import mg.bank.backend.model.referentiel.Statut;
import mg.bank.backend.model.utilisateur.Poste;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.model.validation.EtapeValidation;
import mg.bank.backend.model.validation.ValidationActivite;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.activite.AvancementSousActiviteRepository;
import mg.bank.backend.repository.activite.HistoriqueActiviteRepository;
import mg.bank.backend.repository.projection.activite.ActiviteListRow;
import mg.bank.backend.repository.projection.activite.ActiviteStatistiquesRow;
import mg.bank.backend.repository.projection.activite.AvancementSousActiviteRow;
import mg.bank.backend.repository.projection.activite.ResponsableSuiviRow;
import mg.bank.backend.repository.projection.activite.SousActiviteSuiviRow;
import mg.bank.backend.repository.projection.common.OptionRow;
import mg.bank.backend.repository.projection.referentiel.ObjectifAutocompleteRow;
import mg.bank.backend.repository.referentiel.ObjectifSpecifiqueRepository;
import mg.bank.backend.repository.referentiel.PrioriteRepository;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.repository.referentiel.SiteRepository;
import mg.bank.backend.repository.referentiel.StatutRepository;
import mg.bank.backend.repository.referentiel.TypeActiviteRepository;
import mg.bank.backend.repository.validation.ValidationActiviteRepository;
import mg.bank.backend.service.common.ActiviteQuerySupport;
import mg.bank.backend.service.notification.NotificationService;
import mg.bank.backend.service.utilisateur.CurrentUserService;
import mg.bank.backend.service.validation.EtapeValidationService;
import mg.bank.backend.service.validation.ValidationActiviteService;

@Service
@RequiredArgsConstructor
public class ActiviteService {

        private static final int TAILLE_PAGE_MAX = 100;
        private static final int TAILLE_AUTOCOMPLETE = 10;

        private final ActiviteRepository activiteRepository;
        private final ObjectifSpecifiqueRepository objectifSpecifiqueRepository;
        private final PrioriteRepository prioriteRepository;
        private final TypeActiviteRepository typeActiviteRepository;
        private final SiteRepository siteRepository;
        private final StatutRepository statutRepository;
        private final ServiceRepository serviceRepository;
        private final CurrentUserService currentUserService;
        private final HistoriqueActiviteRepository historiqueActiviteRepository;
        private final ValidationActiviteService validationActiviteService;
        private final ValidationActiviteRepository validationActiviteRepository;
        private final EtapeValidationService etapeValidationService;
        private final AvancementSousActiviteRepository avancementSousActiviteRepository;
        private final NotificationService notificationService;

        // ------------------------------------------------------------------
        // Perimetre
        // ------------------------------------------------------------------
        private Poste posteMetierCourant(Utilisateur utilisateur) {
                Poste poste = utilisateur.getPoste();
                if (poste == null || !Boolean.TRUE.equals(poste.getIsMetier())) {
                        throw new ApiException(
                                        "Poste métier introuvable pour l'utilisateur",
                                        HttpStatus.NOT_FOUND);
                }
                return poste;
        }

        @Transactional(readOnly = true)
        public Map<EtapeValidation, List<Activite>> getActiviteNonValidees() {
                Utilisateur utilisateur = utilisateurCourant();
                if (utilisateur == null) {
                        throw new ApiException("Utilisateur non authentifie", HttpStatus.UNAUTHORIZED);
                }

                Poste poste = posteMetierCourant(utilisateur);

                List<EtapeValidation> etapes = etapeValidationService.getEtapesByPoste(poste.getIdPoste());
                if (etapes.isEmpty()) {
                        return Map.of();
                }

                List<Integer> idsEtapes = etapes.stream()
                                .map(EtapeValidation::getIdEtapeValidation)
                                .toList();

                List<ValidationActivite> enAttente = validationActiviteRepository
                                .findEnAttenteByEtapes(idsEtapes, DecisionValidation.EN_ATTENTE_VALIDATION);

                Map<Integer, EtapeValidation> etapesParId = etapes.stream()
                                .collect(Collectors.toMap(EtapeValidation::getIdEtapeValidation, Function.identity()));

                Map<EtapeValidation, List<Activite>> resultat = new LinkedHashMap<>();
                for (EtapeValidation etape : etapes) {
                        resultat.put(etape, new ArrayList<>());
                }
                for (ValidationActivite v : enAttente) {
                        EtapeValidation etape = etapesParId.get(v.getEtapeValidation().getIdEtapeValidation());
                        if (etape != null &&
                                        (utilisateur.getService() == null
                                                        || v.getActivite().getService().getIdService().equals(
                                                                        utilisateur.getService().getIdService()))
                                        && (utilisateur.getDepartement() == null ||
                                                        v.getActivite().getService().getDepartement().getIdDepartement()
                                                                        .equals(utilisateur.getDepartement()
                                                                                        .getIdDepartement()))) {
                                resultat.get(etape).add(v.getActivite());
                        }
                }
                return resultat;
        }

        @Transactional
        ValidationActivite soumettreRetour(Activite activite, Utilisateur utilisateur, String commentaire) {
                ValidationActivite validation = validationActiviteService
                                .getDerniereEtapeNonValidee(activite.getIdActivite())
                                .orElseThrow(() -> new ApiException("Aucune étape précédente en cours à retourner",
                                                HttpStatus.NOT_FOUND));
                List<Utilisateur> demandeursPrecedents = validationActiviteRepository
                                .findDemandeursByActiviteId(activite.getIdActivite());
                EtapeValidation precedente = etapeValidationService.getEtapePrecedente(validation.getEtapeValidation())
                                .orElseThrow(() -> new ApiException(
                                                "Aucune étape précédente trouvée pour l'étape actuelle",
                                                HttpStatus.BAD_REQUEST));
                validation.setDecision(DecisionValidation.RETOUR_MODIFICATION);
                validation.setDecideur(utilisateur);
                validation.setDateDecision(LocalDateTime.now());
                validationActiviteService.soumettre(validation);

                ValidationActivite validationActivite = ValidationActivite.builder()
                                .activite(activite)
                                .etapeValidation(precedente)
                                .demandeur(utilisateur)
                                .decision(DecisionValidation.EN_ATTENTE_VALIDATION)
                                .dateDemande(LocalDateTime.now())
                                .commentaire(commentaire)
                                .build();
                ValidationActivite soumise = validationActiviteService.soumettre(validationActivite);
                notifierDemandeurs(activite, demandeursPrecedents, "Activité retournée pour modification",
                                "L'activité « " + activite.getDesignation()
                                                + " » a été retournée pour modification.");
                return soumise;

        }

        @Transactional(readOnly = true)
        public List<Activite> getBrouillonsUtilisateurCourant() {
                Utilisateur utilisateur = utilisateurCourant();
                if (utilisateur == null) {
                        throw new ApiException("Utilisateur non authentifie", HttpStatus.UNAUTHORIZED);
                }

                return activiteRepository.findBrouillonsByUtilisateur(
                                StatutsActivite.BROUILLON,
                                utilisateur.getIdUtilisateur());
        }

        @Transactional
        public ValidationActivite soumettre(Integer idActivite, Boolean retour, String commentaire) {
                Activite activite = activiteRepository.findById(idActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Activite introuvable", HttpStatus.NOT_FOUND));
                Utilisateur utilisateur = utilisateurCourant();
                if (utilisateur == null) {
                        throw new ApiException("Utilisateur non authentifie", HttpStatus.UNAUTHORIZED);
                }

                if (retour)
                        return soumettreRetour(activite, utilisateur, commentaire);

                Optional<ValidationActivite> derniereValidation = validationActiviteService
                                .getDerniereValidationValidee(idActivite);
                EtapeValidation etapeValidation = derniereValidation
                                .map(ValidationActivite::getEtapeValidation)
                                .orElse(null);
                Poste poste = posteMetierCourant(utilisateur);
                EtapeValidation prochaineEtape = etapeValidationService
                                .getEtapeSuivante(etapeValidation, activite.getProcedure().getIdProcedure(), poste)
                                .orElseThrow(() -> new ApiException(
                                                "Aucune étape suivante trouvée pour l'étape actuelle",
                                                HttpStatus.BAD_REQUEST));
                ValidationActivite validationActivite = ValidationActivite.builder()
                                .activite(activite)
                                .etapeValidation(prochaineEtape)
                                .demandeur(utilisateur)
                                .decision(DecisionValidation.EN_ATTENTE_VALIDATION)
                                .dateDemande(LocalDateTime.now())
                                .commentaire(commentaire)
                                .build();
                List<Utilisateur> demandeursPrecedents = validationActiviteRepository
                                .findDemandeursByActiviteId(activite.getIdActivite());
                ValidationActivite soumise = validationActiviteService.soumettre(validationActivite);
                notifierDemandeurs(activite, demandeursPrecedents, "Activité soumise à validation",
                                "L'activité « " + activite.getDesignation()
                                                + " » a été soumise à validation.");
                notificationService.notifierEtape(activite, prochaineEtape, "Activité en attente de validation",
                                "L'activité « " + activite.getDesignation() + " » est en attente de votre validation.");
                return soumise;
        }

        private void changerStatut(
                        Activite activite,
                        Statut nouveauStatut,
                        Utilisateur utilisateur,
                        String commentaire) {

                HistoriqueActivite historique = HistoriqueActivite.builder()
                                .activite(activite)
                                .statut(nouveauStatut)
                                .utilisateur(utilisateur)
                                .commentaire(commentaire)
                                .dateChangement(LocalDateTime.now())
                                .build();

                historiqueActiviteRepository.save(historique);
        }

        @Transactional
        public ValidationActivite valider(Integer idActivite, Boolean rejeter, String commentaire) {
                Activite activite = activiteRepository.findById(idActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Activite introuvable", HttpStatus.NOT_FOUND));
                Utilisateur utilisateur = utilisateurCourant();
                if (utilisateur == null) {
                        throw new ApiException("Utilisateur non authentifie", HttpStatus.UNAUTHORIZED);
                }

                Poste poste = posteMetierCourant(utilisateur);

                ValidationActivite validationActivite = validationActiviteService.getDerniereEtapeNonValidee(idActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Aucune étape en cours de validation pour cette activité",
                                                HttpStatus.BAD_REQUEST));
                List<Utilisateur> demandeurs = validationActiviteRepository
                                .findDemandeursByActiviteId(activite.getIdActivite());
                EtapeValidation prochaineEtape = null;

                validationActivite.setDecision(rejeter ? DecisionValidation.REJETE : DecisionValidation.VALIDE);
                validationActivite.setDateDecision(LocalDateTime.now());
                validationActivite.setDecideur(utilisateur);

                if (rejeter) {
                        Optional<ValidationActivite> validation = validationActiviteService
                                        .getDerniereValidation(idActivite);
                        if (validation.isPresent()) {
                                if (DecisionValidation.RETOUR_MODIFICATION.equals(validation.get().getDecision())) {
                                        throw new ApiException(
                                                        "Impossible de rejeter une activité qui a été retournée pour modification",
                                                        HttpStatus.BAD_REQUEST);
                                }
                        }
                        changerStatut(activite, statutRepository.findByCode(StatutsActivite.REJETE)
                                        .orElseThrow(() -> new ApiException(
                                                        "Statut REJETEE introuvable",
                                                        HttpStatus.INTERNAL_SERVER_ERROR)),
                                        utilisateur, commentaire);
                } else {
                        prochaineEtape = etapeValidationService
                                        .getEtapeSuivante(validationActivite.getEtapeValidation(),
                                                        activite.getProcedure().getIdProcedure(), poste)
                                        .orElse(null);
                        if (prochaineEtape == null) {
                                changerStatut(activite, statutRepository.findByCode(StatutsActivite.VALIDEE)
                                                .orElseThrow(() -> new ApiException(
                                                                "Statut VALIDEE introuvable",
                                                                HttpStatus.INTERNAL_SERVER_ERROR)),
                                                utilisateur, commentaire);
                        } else {
                                ValidationActivite prochaineValidation = ValidationActivite.builder()
                                                .activite(activite)
                                                .etapeValidation(prochaineEtape)
                                                .demandeur(utilisateur)
                                                .decision(DecisionValidation.EN_ATTENTE_VALIDATION)
                                                .dateDemande(LocalDateTime.now())
                                                .commentaire(commentaire)
                                                .build();
                                validationActiviteService.soumettre(prochaineValidation);
                        }
                }

                ValidationActivite decision = validationActiviteService.soumettre(validationActivite);
                if (rejeter || prochaineEtape == null) {
                        String message = rejeter
                                        ? "L'activité « " + activite.getDesignation()
                                                        + " » a été rejetée à l'étape « "
                                                        + validationActivite.getEtapeValidation().getDesignation()
                                                        + " »."
                                        : "La validation finale de l'activité « " + activite.getDesignation()
                                                        + " » est terminée : elle est validée.";
                        if (rejeter && commentaire != null && !commentaire.isBlank()) {
                                message += " Commentaire : " + commentaire.trim();
                        }
                        notifierDemandeurs(activite, demandeurs,
                                        rejeter ? "Activité rejetée" : "Activité validée", message);
                }
                if (!rejeter && prochaineEtape != null) {
                        notificationService.notifierEtape(activite, prochaineEtape, "Activité en attente de validation",
                                        "L'activité « " + activite.getDesignation()
                                                        + " » est en attente de votre validation.");
                }
                return decision;
        }

        private void notifierDemandeurs(Activite activite, List<Utilisateur> demandeurs, String titre, String message) {
                demandeurs.forEach(demandeur -> notificationService.notifierUtilisateur(
                                activite, demandeur, titre, message));
        }

        @Transactional(readOnly = true)
        public ApiException refusOuAbsence(Integer idActivite) {
                if (activiteRepository.compterActivite(idActivite) > 0) {
                        return new ApiException(
                                        "Vous n etes pas autorise a consulter cette activite",
                                        HttpStatus.FORBIDDEN);
                }

                return new ApiException("Activite introuvable", HttpStatus.NOT_FOUND);
        }

        @Transactional
        public String prochainCodeNonPta(int annee) {

                if (annee < 1900 || annee > 2100) {
                        throw new ApiException(
                                        "L'année doit être comprise entre 1900 et 2100",
                                        HttpStatus.BAD_REQUEST);
                }

                int dernier = activiteRepository.dernierNumeroNonPta(annee);

                return "A-" + annee + "-" + (dernier + 1);
        }

        public AutocompleteDTO creerObjectifSpecifique(ObjectifSpecifiqueEcritureRequest request) {

                String code = request.getCode().trim();

                if (objectifSpecifiqueRepository.existsByCodeIgnoreCase(code)) {
                        throw new ApiException(
                                        "Un objectif porte déjà le code « " + code + " »",
                                        HttpStatus.CONFLICT);
                }

                ObjectifSpecifique enregistre;

                try {
                        enregistre = objectifSpecifiqueRepository.save(
                                        ObjectifSpecifique.builder()
                                                        .code(code)
                                                        .designation(request.getDesignation().trim())
                                                        .annee(request.getAnnee())
                                                        .build());
                } catch (DataIntegrityViolationException e) {
                        throw new ApiException(
                                        "Un objectif porte déjà le code « " + code + " »",
                                        HttpStatus.CONFLICT);
                }

                return AutocompleteDTO.builder()
                                .id(enregistre.getIdObjectifSpecifique())
                                .code(enregistre.getCode())
                                .libelle(enregistre.getDesignation())
                                .libelleSecondaire(String.valueOf(enregistre.getAnnee()))
                                .annee(enregistre.getAnnee())
                                .prochainNumero(1)
                                .build();
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

                Page<ActiviteListItemDTO> resultat = executerRecherche(filtres, page, size, sort)
                                .map(ActiviteMapper::versDto);

                return PageResponse.from(resultat);
        }

        private Page<ActiviteListRow> executerRecherche(
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

                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
                var args = ActiviteQuerySupport.arguments(filtres, perimetre);

                Sort tri = (sort == null || sort.isUnsorted())
                                ? ActiviteQuerySupport.TRI_PAR_DEFAUT
                                : sort;
                Pageable pageable = PageRequest.of(page, size, ActiviteQuerySupport.assainir(tri));

                return activiteRepository.findActivites(
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
                                pageable);
        }

        // ------------------------------------------------------------------
        // Statistiques
        // ------------------------------------------------------------------

        @Transactional(readOnly = true)
        public ActiviteStatistiquesDTO statistiques(ActiviteFiltreCriteria filtres) {

                PerimetreUtilisateur perimetre = getPerimetre();
                var args = ActiviteQuerySupport.arguments(filtres, perimetre);

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
                                .total(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getTotal()))
                                .nonCommencees(ActiviteQuerySupport
                                                .nulSiAbsent(row == null ? null : row.getNonCommencees()))
                                .enCours(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getEnCours()))
                                .terminees(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getTerminees()))
                                .enRetard(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getEnRetard()))
                                .annulees(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getAnnulees()))
                                .reportees(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getReportees()))
                                .suspendues(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getSuspendues()))
                                .build();
        }

        // ------------------------------------------------------------------
        // Referentiels de filtres
        // ------------------------------------------------------------------

        @Transactional(readOnly = true)
        public ActiviteOptionsDTO options() {

                PerimetreUtilisateur perimetre = getPerimetre();

                List<OptionDTO> services = perimetre.estNiveauDepartement()
                                ? ActiviteQuerySupport.versOptions(serviceRepository
                                                .findOptionsActivesParDepartement(perimetre.getIdDepartement()))
                                : List.of();

                return ActiviteOptionsDTO.builder()
                                .services(services)
                                .priorites(ActiviteQuerySupport.versOptions(prioriteRepository.findOptionsActives()))
                                .typesActivite(ActiviteQuerySupport
                                                .versOptions(typeActiviteRepository.findOptionsActives()))
                                .sites(ActiviteQuerySupport.versOptions(siteRepository.findOptionsActives()))
                                .statuts(ActiviteQuerySupport.versOptions(statutRepository.findOptionsActifs()))
                                .build();
        }

        // ------------------------------------------------------------------
        // Autocompletes
        // ------------------------------------------------------------------

        @Transactional(readOnly = true)
        public List<AutocompleteDTO> autocompleterActivites(String recherche) {

                PerimetreUtilisateur perimetre = getPerimetre();

                List<OptionRow> rows = activiteRepository.autocomplete(
                                ActiviteQuerySupport.videSiBlanc(recherche),
                                ActiviteQuerySupport.motif(recherche),
                                perimetre.getFiltreService(),
                                perimetre.getFiltreDepartement(),
                                TAILLE_AUTOCOMPLETE);

                if (rows == null) {
                        return List.of();
                }

                return rows.stream()
                                .map(r -> AutocompleteDTO.builder()
                                                .id(r.getId())
                                                .code(r.getCode())
                                                .libelle(r.getLibelle())
                                                // La requete selectionne deja la designation du type
                                                // d activite : sans elle, deux activites de meme
                                                // prefixe de code seraient impossibles a distinguer.
                                                .libelleSecondaire(r.getLibelleSecondaire())
                                                .build())
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<AutocompleteDTO> autocompleterObjectifs(String recherche) {

                List<ObjectifAutocompleteRow> rows = objectifSpecifiqueRepository.autocomplete(
                                ActiviteQuerySupport.videSiBlanc(recherche),
                                ActiviteQuerySupport.motif(recherche),
                                TAILLE_AUTOCOMPLETE);

                if (rows == null) {
                        return List.of();
                }

                return rows.stream()
                                .map(r -> AutocompleteDTO.builder()
                                                .id(r.getId())
                                                .code(r.getCode())
                                                .libelle(r.getLibelle())
                                                // L'annee reste le second axe de recherche du
                                                // menu, comme elle l'etait en libelleSecondaire.
                                                .libelleSecondaire(
                                                                r.getAnnee() == null ? null
                                                                                : String.valueOf(r.getAnnee()))
                                                .annee(r.getAnnee())
                                                .prochainNumero(r.getProchainNumero())
                                                .build())
                                .toList();
        }

        public record ContexteSoumission(
                        EtapeValidation etapeCourante,
                        Optional<EtapeValidation> etapeSuivante,
                        Optional<EtapeValidation> etapePrecedente,
                        List<Activite> brouillons) {
        }

        @Transactional(readOnly = true)
        public ContexteSoumission getContexteSoumission() {
                Utilisateur utilisateur = utilisateurCourant();
                if (utilisateur == null) {
                        throw new ApiException("Utilisateur non authentifie", HttpStatus.UNAUTHORIZED);
                }

                Poste poste = posteMetierCourant(utilisateur);

                List<EtapeValidation> etapesPoste = etapeValidationService.getEtapesByPoste(poste.getIdPoste());

                EtapeValidation courante = etapesPoste.isEmpty() ? null : etapesPoste.get(0);

                List<Activite> brouillons = activiteRepository.findBrouillonsByUtilisateur(
                                StatutsActivite.BROUILLON,
                                utilisateur.getIdUtilisateur());

                if (courante == null) {
                        return new ContexteSoumission(null, Optional.empty(), Optional.empty(), brouillons);
                }

                return new ContexteSoumission(
                                courante,
                                etapeValidationService.getEtapeSuivante(courante,
                                                courante.getProcedure().getIdProcedure(), null),
                                etapeValidationService.getEtapePrecedente(courante),
                                brouillons);
        }

        @Transactional(readOnly = true)
        public PageResponse<ActiviteSuiviDTO> rechercherSuivi(
                        ActiviteFiltreCriteria filtres,
                        int page,
                        int size,
                        Sort sort) {

                Page<ActiviteListRow> resultat = executerRecherche(filtres, page, size, sort);

                List<Integer> ids = resultat.getContent().stream()
                                .map(ActiviteListRow::getId)
                                .toList();

                if (ids.isEmpty()) {
                        return PageResponse.from(resultat.map(this::versSuiviSansEnfants));
                }

                // 2 requetes batch, independantes du nombre d'activites de la page.
                Map<Integer, List<SousActiviteSuiviDTO>> sousParActivite = chargerSousActivites(ids);
                Map<Integer, ResponsableSuiviDTO> responsableParActivite = chargerResponsables(ids);

                Page<ActiviteSuiviDTO> enrichi = resultat.map(row -> {
                        ActiviteSuiviDTO base = versSuiviSansEnfants(row);
                        return ActiviteSuiviDTO.builder()
                                        .id(base.getId())
                                        .code(base.getCode())
                                        .reference(base.getReference())
                                        .designation(base.getDesignation())
                                        .dateDebutPrevue(base.getDateDebutPrevue())
                                        .dateFinPrevue(base.getDateFinPrevue())
                                        .dateDebutReelle(base.getDateDebutReelle())
                                        .dateFinReelle(base.getDateFinReelle())
                                        .service(base.getService())
                                        .priorite(base.getPriorite())
                                        .statut(base.getStatut())
                                        .avancement(base.getAvancement())
                                        .sousActivites(sousParActivite.getOrDefault(row.getId(), List.of()))
                                        .responsable(responsableParActivite.get(row.getId()))
                                        .build();
                });

                return PageResponse.from(enrichi);
        }

        private ActiviteSuiviDTO versSuiviSansEnfants(ActiviteListRow row) {
                return ActiviteSuiviDTO.builder()
                                .id(row.getId())
                                .code(row.getCode())
                                .reference(row.getReference())
                                .designation(row.getDesignation())
                                .dateDebutPrevue(row.getDateDebutPrevue())
                                .dateFinPrevue(row.getDateFinPrevue())
                                .dateDebutReelle(row.getDateDebutReelle())
                                .dateFinReelle(row.getDateFinReelle())
                                .service(ReferenceDTO.builder()
                                                .id(row.getServiceId())
                                                .libelle(row.getServiceLibelle())
                                                .build())
                                .priorite(row.getPrioriteId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getPrioriteId())
                                                                .code(row.getPrioriteCode())
                                                                .libelle(row.getPrioriteLibelle())
                                                                .build())
                                .statut(row.getStatutId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getStatutId())
                                                                .code(row.getStatutCode())
                                                                .libelle(row.getStatutLibelle())
                                                                .build())
                                .avancement(row.getAvancement() == null ? 0d : row.getAvancement())
                                .sousActivites(List.of())
                                .build();
        }

        private Map<Integer, List<SousActiviteSuiviDTO>> chargerSousActivites(List<Integer> ids) {
                List<SousActiviteSuiviRow> sousActivites = activiteRepository.findSousActivitesPourActivites(ids);
                if (sousActivites.isEmpty())
                        return Map.of();

                // Une seconde requête batch pour les derniers avancements de TOUTES les
                // sous-activités de la page. Ce n'est pas du N+1 : le nombre de
                // requêtes reste constant quel que soit le nombre de sous-activités.
                List<Integer> idsSous = sousActivites.stream()
                                .map(SousActiviteSuiviRow::getId)
                                .toList();
                Map<Integer, Double> avancementParSous = avancementSousActiviteRepository
                                .findDerniersPourSousActivites(idsSous).stream()
                                .collect(Collectors.toMap(
                                                AvancementSousActiviteRow::getIdSousActivite,
                                                r -> r.getValeur() == null ? 0d : r.getValeur(),
                                                (a, b) -> a));

                return sousActivites.stream()
                                .collect(Collectors.groupingBy(
                                                SousActiviteSuiviRow::getIdActivite,
                                                Collectors.mapping(
                                                                r -> SousActiviteSuiviDTO.builder()
                                                                                .id(r.getId())
                                                                                .code(r.getCode())
                                                                                .designation(r.getDesignation())
                                                                                .avancement(avancementParSous
                                                                                                .getOrDefault(r.getId(),
                                                                                                                0d))
                                                                                .build(),
                                                                Collectors.toList())));
        }

        private Map<Integer, ResponsableSuiviDTO> chargerResponsables(List<Integer> ids) {
                return activiteRepository.findResponsablesActifsPourActivites(ids).stream()
                                .collect(Collectors.toMap(
                                                ResponsableSuiviRow::getIdActivite,
                                                r -> ResponsableSuiviDTO.builder()
                                                                .idUtilisateur(r.getIdUtilisateur())
                                                                .nom(r.getNom())
                                                                .prenom(r.getPrenom())
                                                                .build(),
                                                (a, b) -> a));
        }

        public Utilisateur utilisateurCourant() {
                return currentUserService.get();
        }

        @Transactional(readOnly = true)
        public PerimetreUtilisateur getPerimetre() {
                return currentUserService.getPerimetre();
        }

        @Transactional(readOnly = true)
        public Double calculerAvancementActivite(Integer idActivite) {
                return activiteRepository.findAvancementActivite(idActivite);
        }

}
