package mg.bank.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ActiviteFiltreCriteria;
import mg.bank.backend.dto.ActiviteListItemDTO;
import mg.bank.backend.dto.ActiviteOptionsDTO;
import mg.bank.backend.dto.ActiviteStatistiquesDTO;
import mg.bank.backend.dto.AutocompleteDTO;
import mg.bank.backend.dto.ObjectifSpecifiqueEcritureRequest;
import mg.bank.backend.dto.OptionDTO;
import mg.bank.backend.dto.PageResponse;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.dto.ReferenceDTO;
import mg.bank.backend.enums.DecisionValidation;
import mg.bank.backend.enums.StatutActiviteEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Activite;
import mg.bank.backend.model.EtapeValidation;
import mg.bank.backend.model.HistoriqueActivite;
import mg.bank.backend.model.ObjectifSpecifique;
import mg.bank.backend.model.Poste;
import mg.bank.backend.model.Statut;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.model.ValidationActivite;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.HistoriqueActiviteRepository;
import mg.bank.backend.repository.ObjectifSpecifiqueRepository;
import mg.bank.backend.repository.PrioriteRepository;
import mg.bank.backend.repository.UtilisateurRepository;
import mg.bank.backend.repository.ServiceRepository;
import mg.bank.backend.repository.SiteRepository;
import mg.bank.backend.repository.StatutRepository;
import mg.bank.backend.repository.TypeActiviteRepository;
import mg.bank.backend.repository.projection.ActiviteListRow;
import mg.bank.backend.repository.projection.ActiviteStatistiquesRow;
import mg.bank.backend.repository.projection.ObjectifAutocompleteRow;
import mg.bank.backend.repository.projection.OptionRow;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.repository.PosteRepository;
import mg.bank.backend.repository.ValidationActiviteRepository;
import java.util.function.Function;

/**
 * Service de consultation des activites, et source de verite du perimetre.
 *
 * Les ecritures sont ailleurs : ActiviteEcritureService pour la redaction, et
 * LivrableEcritureService pour le depot de pieces. Ce service reste ce qu il
 * annonce, et getPerimetre et utilisateurCourant restent les seules
 * reconstructions de "qui appelle et jusqu ou".
 *
 * Toute la logique metier vit ici : les repositories ne contiennent que des
 * requetes. C'est notamment le cas de trois decisions :
 *
 * 1. le perimetre, reconstruit depuis l'utilisateur authentifie et jamais
 * depuis un parametre HTTP ;
 * 2. les bornes de mois du trimestre, calculees ici pour que le SQL reste
 * sappable sur date_debut_prevue ;
 * 3. la liste des codes terminaux, injectee dans le SQL sous forme de
 * liste chainee puis scindee par la base.
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
        private static final Sort TRI_PAR_DEFAUT = Sort.by(Sort.Direction.DESC, "dateCreationTri");

        private final ActiviteRepository activiteRepository;
        private final ObjectifSpecifiqueRepository objectifSpecifiqueRepository;
        private final PrioriteRepository prioriteRepository;
        private final TypeActiviteRepository typeActiviteRepository;
        private final SiteRepository siteRepository;
        private final StatutRepository statutRepository;
        private final ServiceRepository serviceRepository;
        private final UtilisateurRepository utilisateurRepository;
        private final HistoriqueActiviteRepository historiqueActiviteRepository;
        private final ValidationActiviteService validationActiviteService;
        private final ValidationActiviteRepository validationActiviteRepository;
        private final EtapeValidationService etapeValidationService;
        private final PosteRepository posteRepository;

        // ------------------------------------------------------------------
        // Perimetre
        // ------------------------------------------------------------------

        /**
         * Poste métier de l'utilisateur, ou une 404 explicite.
         *
         * Le modèle porte désormais un poste unique sur l'utilisateur : plus besoin
         * de le chercher en base, il est déjà chargé avec l'utilisateur. La méthode
         * factorise les quatre points qui exigent un poste métier — refuser au même
         * endroit évite que l'un d'eux oublie la condition isMetier et laisse un
         * utilisateur support valider des activités.
         */
        private Poste posteMetierCourant(Utilisateur utilisateur) {
                Poste poste = utilisateur.getPoste();
                if (poste == null || !Boolean.TRUE.equals(poste.getIsMetier())) {
                        throw new ApiException(
                                        "Poste métier introuvable pour l'utilisateur",
                                        HttpStatus.NOT_FOUND);
                }
                return poste;
        }

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

                // 1 seule requête : toutes les validations EN_ATTENTE pour toutes les étapes du
                // poste,
                // avec activite + ses relations + etapeValidation déjà fetchés.
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
                                                || v.getActivite().getService().getIdService().equals(utilisateur.getService().getIdService())) 
                                        && (utilisateur.getDepartement() == null ||
                                                v.getActivite().getService().getDepartement().getIdDepartement().equals(utilisateur.getDepartement().getIdDepartement()))) {
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
                return validationActiviteService.soumettre(validationActivite);

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
                EtapeValidation prochaineEtape = etapeValidationService.getEtapeSuivante(etapeValidation, activite.getProcedure().getIdProcedure(), poste)
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
                return validationActiviteService.soumettre(validationActivite);
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
                        EtapeValidation prochaineEtape = etapeValidationService
                                        .getEtapeSuivante(validationActivite.getEtapeValidation(), activite.getProcedure().getIdProcedure(), poste).orElse(null);
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

                return validationActiviteService.soumettre(validationActivite);
        }

        /**
         * Appelant, lu dans le contexte de securite.
         *
         * Public pour la meme raison que getPerimetre : c est la seule source de
         * l identite de l appelant dans le projet. Une ecriture doit savoir QUI
         * soumet, pour l inscrire dans l historique ; si elle reconstruisait
         * elle-meme cette identite, deux lectures de la regle pourraient
         * diverger.
         */
        public Utilisateur utilisateurCourant() {
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

        /**
         * Departage 404 / 403, partage par toutes les lectures et ecritures.
         *
         * A n'appeler qu apres l echec d une requete RESTREINTE au perimetre :
         * cette requete a deja repondu "rien", et la question reste de savoir si
         * c est parce que la donnee n existe pas ou parce qu elle appartient a
         * quelqu un d autre. Sans cette verification supplementaire, un 403
         * paraitrait inexistant, et l appelant ne pourrait plus distinguer une
         * faute de frappe d un acces refuse.
         *
         * Le message du 403 ne nomme ni l activite ni son service : il confirme
         * l acces refuse, pas la position de la donnee.
         */
        @Transactional(readOnly = true)
        public ApiException refusOuAbsence(Integer idActivite) {
                if (activiteRepository.compterActivite(idActivite) > 0) {
                        return new ApiException(
                                        "Vous n etes pas autorise a consulter cette activite",
                                        HttpStatus.FORBIDDEN);
                }

                return new ApiException("Activite introuvable", HttpStatus.NOT_FOUND);
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

        /**
         * Creer un objectif specifique depuis le formulaire d'activite.
         *
         * UNE EXCEPTION A LA REGLE DE CE SERVICE
         *
         * Ce service est annonce comme celui de la consultation, et la creation
         * d'un objectif en est une. Elle n'ouvre pas pour autant un second
         * service : l'objectif n'existe dans l'application qu'a travers le
         * formulaire d'activite -- c'est le seul ecran qui le choisit -- et
         * l'ecrire ailleurs disperserait une regle qui n'a qu'un appelant. La
         * regle d'ecriture reste celle du projet : tout le metier est ici, le
         * repository ne fait qu'enregistrer.
         *
         * LA REPONSE EST UN OBJECTIF, ET PAS UN IDENTIFIANT
         *
         * Le formulaire selectionne l'objectif qu'il vient de creer, sans repasser
         * par l'autocomplete : c'est cette forme, enrichie du meme libelle et du
         * meme `prochainNumero` que propose l'autocomplete, qui evite un aller-retour
         * et une saisie qui peut reussir a l'ecran et echouer en base.
         *
         * `prochainNumero` vaut 1 : un objectif ne vient que d'etre cree, il ne
         * porte aucune activite, et la premiere activite qu'on y rattache sera la
         * numero 1. C'est la valeur qu'on proposerait si l'on relisait l'autocomplete.
         */
        @Transactional
        /**
         * Code propose pour une activite NON PTA : A-<annee>-<suite du dernier>.
         *
         * UNE NON PTA N A NI OBJECTIF NI NUMERO D ACTIVITE
         *
         * Le code d un PTA se deduit de l objectif (A-<annee>-<objectif>-<n>), donc
         * l objectif permet de le proposer. Une NON PTA n en a pas : sans cette
         * regle, le champ resterait vide et l utilisateur devrait inventer un code
         * qui ne se rappelle de rien -- alors que la regle du jeu de donnees est
         * simple, une sequence par annee, A-2027-01 a A-2027-50.
         *
         * LE CODE COMPLET EST RENVOYE, ET PAS LE NUMERO
         *
         * Le format appartient a la base : c est elle qui porte la regle, et le
         * formulaire comme une lecture du code existant evitent d avoir a la
         * reconcevoir. Le backend renvoie donc la valeur prete a etre affichee,
         * ce qui laisse au front une seule chose a faire.
         *
         * La valeur reste une PROPOSITION : le champ est modifiable, et le backend
         * ne controle que l unicite. Deux utilisateurs qui ouvrent le formulaire en
         * meme temps peuvent voir le meme code ; le second enregistrement est alors
         * refuse pour code deja utilise, ce qui est une erreur de saisie et non une
         * panne.
         *
         * L annee est demandee et non relue ailleurs : une NON PTA n a pas d
         * objectif, donc rien dans l activite ne la designe avant que l
         * utilisateur ne renseigne sa date de debut. C est au formulaire de
         * savoir sur quelle annee il travaille.
         */
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
                        // Refuse ici et non sur la violation de la contrainte unique, qui
                        // remonterait en 500 : l'utilisateur a saisi un code deja pris,
                        // c'est une erreur de saisie et non une panne.
                        throw new ApiException(
                                        "Un objectif porte déjà le code « " + code + " »",
                                        HttpStatus.CONFLICT);
                }

                ObjectifSpecifique enregistre;

                try {
                        // Identifiant genere par la base : le INSERT part ici, ce qui permet
                        // de traduire la violation de contrainte.
                        enregistre = objectifSpecifiqueRepository.save(
                                        ObjectifSpecifique.builder()
                                                        .code(code)
                                                        .designation(request.getDesignation().trim())
                                                        .annee(request.getAnnee())
                                                        .build());
                } catch (DataIntegrityViolationException e) {
                        // Deux creations du meme code au meme instant franchissent toutes
                        // deux le controle precedent : c'est la meme erreur de saisie que
                        // ci-dessus, pas une panne, et elle doit se lire pareil.
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
                                .objectifSpecifique(row.getOsId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getOsId())
                                                                .code(row.getOsCode())
                                                                .libelle(row.getOsDesignation())
                                                                .annee(row.getOsAnnee())
                                                                .build())
                                .service(ReferenceDTO.builder()
                                                .id(row.getServiceId())
                                                .libelle(row.getServiceLibelle())
                                                .build())
                                .typeActivite(row.getTypeActiviteId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getTypeActiviteId())
                                                                .libelle(row.getTypeActiviteLibelle())
                                                                .build())
                                .site(row.getSiteId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getSiteId())
                                                                .libelle(row.getSiteLibelle())
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
                                .build();
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

                /*
                 * Un poste metier n'intervient qu'a une seule etape de la procedure,
                 * mais la requete en renvoie une liste : on prend la premiere par
                 * niveau. Si un jour un poste devait decider a plusieurs etapes, il
                 * faudra choisir laquelle -- la regle actuelle n'en prevoit qu'une.
                 */
                EtapeValidation courante = etapesPoste.isEmpty() ? null : etapesPoste.get(0);

                List<Activite> brouillons = activiteRepository.findBrouillonsByUtilisateur(
                                StatutsActivite.BROUILLON,
                                utilisateur.getIdUtilisateur());

                if (courante == null) {
                        return new ContexteSoumission(null, Optional.empty(), Optional.empty(), brouillons);
                }

                return new ContexteSoumission(
                                courante,
                                etapeValidationService.getEtapeSuivante(courante, courante.getProcedure().getIdProcedure(), null),
                                etapeValidationService.getEtapePrecedente(courante),
                                brouillons);
        }

}
