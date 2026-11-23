package mg.bank.backend.service.planaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.affectation.AffectationPlanActionDTO;
import mg.bank.backend.dto.common.PageResponse;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.dto.planaction.AvancementPlanActionDTO;
import mg.bank.backend.dto.planaction.AvancementPlanActionDetailDTO;
import mg.bank.backend.dto.planaction.AvancementSaisieRequest;
import mg.bank.backend.dto.planaction.OrigineLienDTO;
import mg.bank.backend.dto.planaction.PlanActionDetailDTO;
import mg.bank.backend.dto.planaction.PlanActionEcritureRequest;
import mg.bank.backend.dto.planaction.PlanActionEcritureResponse;
import mg.bank.backend.dto.planaction.PlanActionListItemDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.affectation.AffectationPlanAction;
import mg.bank.backend.model.origine.Origine;
import mg.bank.backend.model.planaction.AvancementPlanAction;
import mg.bank.backend.model.planaction.PlanAction;
import mg.bank.backend.model.planaction.PlanActionOrigine;
import mg.bank.backend.model.referentiel.Statut;
import mg.bank.backend.model.utilisateur.Role;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.affectation.AffectationPlanActionRepository;
import mg.bank.backend.repository.origine.OrigineRepository;
import mg.bank.backend.repository.planaction.AvancementPlanActionRepository;
import mg.bank.backend.repository.planaction.PlanActionOrigineRepository;
import mg.bank.backend.repository.planaction.PlanActionRepository;
import mg.bank.backend.repository.projection.affectation.AffectationPlanActionRow;
import mg.bank.backend.repository.projection.planaction.AvancementPlanActionRow;
import mg.bank.backend.repository.projection.planaction.PlanActionListRow;
import mg.bank.backend.repository.projection.planaction.PlanActionOrigineDetailRow;
import mg.bank.backend.repository.referentiel.StatutRepository;
import mg.bank.backend.repository.utilisateur.RoleRepository;
import mg.bank.backend.repository.utilisateur.UtilisateurRepository;
import mg.bank.backend.service.activite.ActiviteService;
import mg.bank.backend.service.common.PorteeService;
import mg.bank.backend.utils.ReferenceMapper;

/**
 * Plans d'action — lecture, creation, detail.
 *
 * Le workflow de validation (soumettre, valider, a-valider) est livre
 * separement : il necessite la procedure dediee aux plans d'action.
 *
 * A la creation, le plan est en statut BROUILLON. Seul le RESPONSABLE
 * affecte voit son brouillon dans /mes-brouillons. Une fois soumis et
 * valide, le plan passe aux statuts NON_COMMENCEE / EN_COURS / TERMINEE
 * et devient visible dans la liste principale.
 */
@Service
@RequiredArgsConstructor
public class PlanActionService {

        private static final String STATUT_BROUILLON = "BROUILLON";
        private static final String ROLE_RESPONSABLE = "RESPONSABLE";
        private static final String[] STATUTS_ACTIFS = { "NON_COMMENCEE", "EN_COURS", "TERMINEE" };
        private static final int TAILLE_PAGE_MAX = 100;

        private final PlanActionRepository planActionRepository;
        private final PlanActionOrigineRepository planActionOrigineRepository;
        private final AffectationPlanActionRepository affectationPlanActionRepository;
        private final AvancementPlanActionRepository avancementPlanActionRepository;
        private final OrigineRepository origineRepository;
        private final RoleRepository roleRepository;
        private final StatutRepository statutRepository;
        private final UtilisateurRepository utilisateurRepository;

        private final ActiviteService activiteService;
        private final PorteeService porteeService;

        // ------------------------------------------------------------------
        // Liste
        // ------------------------------------------------------------------

        @Transactional(readOnly = true)
        public PageResponse<PlanActionListItemDTO> rechercher(
                        String statutCode, String search, Integer annee,
                        java.time.LocalDate dateDebut, java.time.LocalDate dateFin,
                        Integer origineId, String typeOrigine, Integer responsableId,
                        int page, int size, Sort sort) {

                if (page < 0) {
                        throw new ApiException("Le numéro de page ne peut pas être négatif",
                                        HttpStatus.BAD_REQUEST);
                }
                if (size < 1 || size > TAILLE_PAGE_MAX) {
                        throw new ApiException(
                                        "La taille de page doit être comprise entre 1 et " + TAILLE_PAGE_MAX,
                                        HttpStatus.BAD_REQUEST);
                }

                Sort tri = (sort == null || sort.isUnsorted())
                                ? Sort.by(Sort.Direction.DESC, "dateDebutPrevue")
                                : sort;
                Pageable pageable = PageRequest.of(page, size, tri);

                String statutFiltre = (statutCode == null || statutCode.isBlank())
                                ? null
                                : statutCode.trim();
                String statutsParDefaut = (statutFiltre == null)
                                ? String.join(",", STATUTS_ACTIFS)
                                : null;
                PerimetreUtilisateur perimetre = activiteService.getPerimetre();

                Page<PlanActionListItemDTO> resultat = planActionRepository.findPlans(
                                statutFiltre,
                                statutsParDefaut,
                                motif(search),
                                annee,
                                dateDebut,
                                dateFin,
                                origineId,
                                typeOrigine,
                                responsableId,
                                perimetre.getFiltreService(),
                                perimetre.getFiltreDepartement(),
                                perimetre.getIdUtilisateurPortee(),
                                pageable)
                                .map(this::versListItem);

                return PageResponse.from(resultat);
        }

        @Transactional(readOnly = true)
        public PageResponse<PlanActionListItemDTO> aValider(int page, int size, Sort sort) {
                return rechercher("EN_ATTENTE_VALIDATION", null, null, null, null,
                                null, null, null, page, size, sort);
        }

        @Transactional(readOnly = true)
        public PageResponse<PlanActionListItemDTO> aDemarrer(int page, int size, Sort sort) {
                return rechercher("NON_COMMENCEE", null, null, null, null,
                                null, null, null, page, size, sort);
        }

        @Transactional
        public PlanActionDetailDTO soumettre(Integer idPlanAction, String commentaire) {
                PlanActionListRow row = chargerPlanDansPerimetre(idPlanAction);

                if (!"BROUILLON".equals(row.getStatutCode())) {
                        throw new ApiException(
                                        "Seul un brouillon peut être soumis à la validation.",
                                        HttpStatus.CONFLICT);
                }

                PlanAction plan = planActionRepository.findById(idPlanAction)
                                .orElseThrow(() -> new ApiException(
                                                "Plan d'action introuvable.", HttpStatus.NOT_FOUND));

                Statut statut = statutRepository.findByCode("EN_ATTENTE_VALIDATION")
                                .orElseThrow(() -> new ApiException(
                                                "Statut EN_ATTENTE_VALIDATION introuvable en base.",
                                                HttpStatus.INTERNAL_SERVER_ERROR));

                Utilisateur auteur = activiteService.utilisateurCourant();

                avancementPlanActionRepository.save(AvancementPlanAction.builder()
                                .valeurPourcentage(java.math.BigDecimal.ZERO)
                                .commentaire((commentaire == null || commentaire.isBlank())
                                                ? "Soumission à la validation"
                                                : commentaire.trim())
                                .dateChangement(LocalDateTime.now())
                                .utilisateur(auteur)
                                .statut(statut)
                                .planAction(plan)
                                .build());

                return detail(idPlanAction);
        }

        @Transactional
        public PlanActionDetailDTO valider(Integer idPlanAction, boolean rejeter, String commentaire) {
                PlanActionListRow row = chargerPlanDansPerimetre(idPlanAction);

                if (!"EN_ATTENTE_VALIDATION".equals(row.getStatutCode())) {
                        throw new ApiException(
                                        "Ce plan d'action n'est pas en attente de validation.",
                                        HttpStatus.CONFLICT);
                }

                PlanAction plan = planActionRepository.findById(idPlanAction)
                                .orElseThrow(() -> new ApiException(
                                                "Plan d'action introuvable.", HttpStatus.NOT_FOUND));

                String codeStatut = rejeter ? "BROUILLON" : "NON_COMMENCEE";
                Statut statut = statutRepository.findByCode(codeStatut)
                                .orElseThrow(() -> new ApiException(
                                                "Statut " + codeStatut + " introuvable en base.",
                                                HttpStatus.INTERNAL_SERVER_ERROR));

                Utilisateur decideur = activiteService.utilisateurCourant();

                avancementPlanActionRepository.save(AvancementPlanAction.builder()
                                .valeurPourcentage(java.math.BigDecimal.ZERO)
                                .commentaire((commentaire == null || commentaire.isBlank())
                                                ? (rejeter ? "Plan rejeté" : "Plan validé")
                                                : commentaire.trim())
                                .dateChangement(LocalDateTime.now())
                                .utilisateur(decideur)
                                .statut(statut)
                                .planAction(plan)
                                .build());

                return detail(idPlanAction);
        }


        @Transactional(readOnly = true)
        public PageResponse<PlanActionListItemDTO> mesBrouillons(int page, int size, Sort sort) {
                Utilisateur utilisateur = activiteService.utilisateurCourant();

                Sort tri = (sort == null || sort.isUnsorted())
                                ? Sort.by(Sort.Direction.DESC, "dateDebutPrevue")
                                : sort;
                Pageable pageable = PageRequest.of(page, size, tri);

                Page<PlanActionListItemDTO> resultat = planActionRepository
                                .findMesBrouillons(utilisateur.getIdUtilisateur(), pageable)
                                .map(this::versListItem);

                return PageResponse.from(resultat);
        }

        private PlanActionListRow chargerPlanDansPerimetre(Integer id) {
                PerimetreUtilisateur perimetre = activiteService.getPerimetre();
                return planActionRepository.findByIdWithDetails(
                                id,
                                perimetre.getFiltreService(),
                                perimetre.getFiltreDepartement(),
                                perimetre.getIdUtilisateurPortee())
                                .orElseThrow(() -> new ApiException(
                                                "Plan d'action introuvable.", HttpStatus.NOT_FOUND));
        }

        @Transactional(readOnly = true)
        public PlanActionDetailDTO detail(Integer id) {
                PlanActionListRow row = chargerPlanDansPerimetre(id);

                PlanActionListItemDTO base = versListItem(row);

                return PlanActionDetailDTO.builder()
                                .id(base.getId())
                                .code(base.getCode())
                                .designation(base.getDesignation())
                                .dateDebutPrevue(base.getDateDebutPrevue())
                                .dateFinPrevue(base.getDateFinPrevue())
                                .dateDebutReelle(base.getDateDebutReelle())
                                .dateFinReelle(base.getDateFinReelle())
                                .statut(base.getStatut())
                                .avancement(base.getAvancement())
                                .responsable(base.getResponsable())
                                .origines(planActionOrigineRepository.findByPlanAction(id)
                                                .stream().map(this::versOrigineLien).toList())
                                .affectations(affectationPlanActionRepository.findHistorique(id)
                                                .stream().map(this::versAffectation).toList())
                                .historique(avancementPlanActionRepository.findHistorique(id)
                                                .stream().map(this::versAvancement).toList())
                                .build();
        }

        // ------------------------------------------------------------------
        // Creation
        // ------------------------------------------------------------------

        @Transactional
        public PlanActionEcritureResponse creer(PlanActionEcritureRequest request) {

                if (!request.getDateFinPrevue().isAfter(request.getDateDebutPrevue())
                                && !request.getDateFinPrevue().isEqual(request.getDateDebutPrevue())) {
                        throw new ApiException(
                                        "La date de fin prévue doit être supérieure ou égale à la date de début.",
                                        HttpStatus.BAD_REQUEST);
                }

                // Code : fourni ou genere
                String code;
                if (request.getCode() != null && !request.getCode().isBlank()) {
                        code = request.getCode().trim();
                        if (planActionRepository.existsByCode(code)) {
                                throw new ApiException(
                                                "Un plan d'action porte déjà le code « " + code + " ».",
                                                HttpStatus.CONFLICT);
                        }
                } else {
                        int annee = request.getDateDebutPrevue().getYear();
                        int numero = planActionRepository.dernierNumeroPourAnnee(annee) + 1;
                        code = "PA-" + annee + "-" + numero;
                        while (planActionRepository.existsByCode(code)) {
                                numero++;
                                code = "PA-" + annee + "-" + numero;
                        }
                }

                // Responsable obligatoire
                Utilisateur responsable = utilisateurRepository.findById(request.getResponsableId())
                                .orElseThrow(() -> new ApiException(
                                                "Utilisateur introuvable.", HttpStatus.BAD_REQUEST));
                if (!Boolean.TRUE.equals(responsable.getActif())) {
                        throw new ApiException("L'utilisateur n'est pas actif.", HttpStatus.BAD_REQUEST);
                }
                porteeService.exigerUtilisateur(responsable);

                Role roleResponsable = roleRepository.findByCodeIgnoreCase(ROLE_RESPONSABLE)
                                .orElseThrow(() -> new ApiException(
                                                "Rôle RESPONSABLE introuvable en base.",
                                                HttpStatus.INTERNAL_SERVER_ERROR));

                // Origines
                List<Origine> origines = request.getOrigineIds().stream()
                                .distinct()
                                .map(idOrigine -> origineRepository.findById(idOrigine)
                                                .orElseThrow(() -> new ApiException(
                                                                "Origine introuvable : id=" + idOrigine + ".",
                                                                HttpStatus.BAD_REQUEST)))
                                .toList();

                if (origines.isEmpty()) {
                        throw new ApiException(
                                        "Au moins une origine est obligatoire.",
                                        HttpStatus.BAD_REQUEST);
                }
                if (origines.stream().anyMatch(this::origineHorsPerimetre)) {
                        throw new ApiException(
                                        "Vous n'êtes pas autorisé à utiliser ces signalements.",
                                        HttpStatus.FORBIDDEN);
                }

                // Statuts
                Statut statutBrouillon = statutRepository.findByCode(STATUT_BROUILLON)
                                .orElseThrow(() -> new ApiException(
                                                "Statut BROUILLON introuvable en base.",
                                                HttpStatus.INTERNAL_SERVER_ERROR));

                Utilisateur auteur = activiteService.utilisateurCourant();

                // 1. Plan
                PlanAction plan = planActionRepository.save(PlanAction.builder()
                                .code(code)
                                .designation(request.getDesignation().trim())
                                .dateDebutPrevue(request.getDateDebutPrevue())
                                .dateFinPrevue(request.getDateFinPrevue())
                                .build());

                // 2. Origines
                for (Origine origine : origines) {
                        planActionOrigineRepository.save(PlanActionOrigine.builder()
                                        .idPlanAction(plan.getIdPlanAction())
                                        .idOrigine(origine.getIdOrigine())
                                        .estPrincipale(Boolean.FALSE)
                                        .build());
                }

                // 3. Affectation RESPONSABLE
                affectationPlanActionRepository.save(AffectationPlanAction.builder()
                                .dateAffectation(LocalDateTime.now())
                                .planAction(plan)
                                .utilisateur(responsable)
                                .role(roleResponsable)
                                .build());

                // 4. Premier avancement = statut BROUILLON, valeur 0
                avancementPlanActionRepository.save(AvancementPlanAction.builder()
                                .valeurPourcentage(BigDecimal.ZERO)
                                .commentaire("Création du plan d'action")
                                .dateChangement(LocalDateTime.now())
                                .utilisateur(auteur)
                                .statut(statutBrouillon)
                                .planAction(plan)
                                .build());

                return PlanActionEcritureResponse.builder()
                                .id(plan.getIdPlanAction())
                                .code(plan.getCode())
                                .designation(plan.getDesignation())
                                .statutInitial(STATUT_BROUILLON)
                                .build();
        }

        private boolean origineHorsPerimetre(Origine origine) {
                if (porteeService.porteeTous()) {
                        return false;
                }
                if (porteeService.utilisateurAccessible(origine.getUtilisateur())) {
                        return false;
                }
                return origineRepository.findActiviteIdsByOrigine(origine.getIdOrigine()).stream()
                                .noneMatch(porteeService::activiteAccessible);
        }

        private PlanActionListItemDTO versListItem(PlanActionListRow row) {
                return PlanActionListItemDTO.builder()
                                .id(row.getId())
                                .code(row.getCode())
                                .designation(row.getDesignation())
                                .dateDebutPrevue(row.getDateDebutPrevue())
                                .dateFinPrevue(row.getDateFinPrevue())
                                .dateDebutReelle(row.getDateDebutReelle())
                                .dateFinReelle(row.getDateFinReelle())
                                .statut(row.getStatutId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getStatutId())
                                                                .code(row.getStatutCode())
                                                                .libelle(row.getStatutLibelle())
                                                                .build())
                                .avancement(row.getAvancement() == null ? BigDecimal.ZERO : row.getAvancement())
                                .responsable(row.getResponsableId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getResponsableId())
                                                                .libelle(row.getResponsablePrenom() + " "
                                                                                + row.getResponsableNom())
                                                                .build())
                                .build();
        }

        private OrigineLienDTO versOrigineLien(PlanActionOrigineDetailRow row) {
                return OrigineLienDTO.builder()
                                .id(row.getIdOrigine())
                                .code(row.getCode())
                                .designation(row.getDesignation())
                                .typeOrigine(row.getTypeOrigine())
                                .estPrincipale(row.getEstPrincipale())
                                .build();
        }

        private AffectationPlanActionDTO versAffectation(AffectationPlanActionRow row) {
                return AffectationPlanActionDTO.builder()
                                .id(row.getId())
                                .dateAffectation(row.getDateAffectation())
                                .dateDesaffectation(row.getDateDesaffectation())
                                .role(ReferenceDTO.builder()
                                                .id(row.getRoleId())
                                                .code(row.getRoleCode())
                                                .libelle(row.getRoleLibelle())
                                                .build())
                                .utilisateur(ReferenceMapper.fromUtilisateur(
                                                row.getUtilisateurId(), row.getUtilisateurNom(),
                                                row.getUtilisateurPrenom()))
                                .active(row.getDateDesaffectation() == null)
                                .build();
        }

        private AvancementPlanActionDTO versAvancement(AvancementPlanActionRow row) {
                return AvancementPlanActionDTO.builder()
                                .id(row.getId())
                                .valeurPourcentage(row.getValeurPourcentage())
                                .commentaire(row.getCommentaire())
                                .dateChangement(row.getDateChangement())
                                .statut(row.getStatutId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getStatutId())
                                                                .code(row.getStatutCode())
                                                                .libelle(row.getStatutLibelle())
                                                                .build())
                                .utilisateur(ReferenceMapper.fromUtilisateur(
                                                row.getUtilisateurId(), row.getUtilisateurNom(),
                                                row.getUtilisateurPrenom()))
                                .build();
        }

        private String motif(String search) {
                if (search == null || search.isBlank())
                        return null;
                return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        }

        // ------------------------------------------------------------------
        // Suivi d'avancement
        // ------------------------------------------------------------------

        private static final Set<String> ROLES_SAISIE = Set.of("RESPONSABLE", "SUPPLEANT");
        private static final Set<String> STATUTS_NON_SAISISSABLES = Set.of("BROUILLON", "ANNULEE");


        @Transactional(readOnly = true)
        public List<AvancementPlanActionDetailDTO> listerAvancements(Integer idPlanAction) {
                porteeService.exigerPlanAction(idPlanAction);
                PlanAction plan = planActionRepository.findById(idPlanAction)
                                .orElseThrow(() -> new ApiException(
                                                "Plan d'action introuvable.", HttpStatus.NOT_FOUND));

                List<AvancementPlanActionRow> rows = avancementPlanActionRepository
                                .findHistorique(plan.getIdPlanAction());

                List<AvancementPlanActionDetailDTO> resultat = new java.util.ArrayList<>();
                for (int i = 0; i < rows.size(); i++) {
                        resultat.add(versAvancementDetail(rows.get(i), i == 0));
                }
                return resultat;
        }

        /**
         * Derniere valeur d'avancement.
         */
        @Transactional(readOnly = true)
        public AvancementPlanActionDetailDTO avancementCourant(Integer idPlanAction) {
                porteeService.exigerPlanAction(idPlanAction);
                planActionRepository.findById(idPlanAction)
                                .orElseThrow(() -> new ApiException(
                                                "Plan d'action introuvable.", HttpStatus.NOT_FOUND));

                AvancementPlanActionRow dernier = avancementPlanActionRepository
                                .findDernier(idPlanAction)
                                .orElseThrow(() -> new ApiException(
                                                "Aucun avancement enregistré pour ce plan.",
                                                HttpStatus.NOT_FOUND));

                return versAvancementDetail(dernier, true);
        }

        @Transactional
        public AvancementPlanActionDetailDTO saisirAvancement(
                        Integer idPlanAction, AvancementSaisieRequest request) {

                porteeService.exigerPlanAction(idPlanAction);
                PlanAction plan = planActionRepository.findById(idPlanAction)
                                .orElseThrow(() -> new ApiException(
                                                "Plan d'action introuvable.", HttpStatus.NOT_FOUND));

                Utilisateur utilisateur = activiteService.utilisateurCourant();

                // 1. Verifier que l'utilisateur est RESPONSABLE ou SUPPLEANT actif
                verifierRoleSaisie(idPlanAction, utilisateur.getIdUtilisateur());

                // 2. Verifier le statut courant
                AvancementPlanActionRow dernier = avancementPlanActionRepository
                                .findDernier(idPlanAction)
                                .orElseThrow(() -> new ApiException(
                                                "Aucun avancement enregistré pour ce plan.",
                                                HttpStatus.INTERNAL_SERVER_ERROR));

                String statutCourant = dernier.getStatutCode();
                if (statutCourant != null && STATUTS_NON_SAISISSABLES.contains(statutCourant)) {
                        throw new ApiException(
                                        "Aucune saisie d'avancement n'est possible sur un plan au statut « "
                                                        + statutCourant.toLowerCase() + " ».",
                                        HttpStatus.CONFLICT);
                }

                // 3. Calculer le nouveau statut selon la valeur
                java.math.BigDecimal valeur = request.getValeurPourcentage();
                String codeNouveauStatut = calculerStatut(valeur);

                Statut statutNouveau = statutRepository.findByCode(codeNouveauStatut)
                                .orElseThrow(() -> new ApiException(
                                                "Statut « " + codeNouveauStatut + " » introuvable en base.",
                                                HttpStatus.INTERNAL_SERVER_ERROR));

                // 4. Mettre a jour les dates reelles du plan si necessaire
                boolean datesModifiees = false;

                if (valeur.compareTo(java.math.BigDecimal.ZERO) > 0
                                && plan.getDateDebutReelle() == null) {
                        plan.setDateDebutReelle(LocalDate.now());
                        datesModifiees = true;
                }

                if (valeur.compareTo(new java.math.BigDecimal("100")) == 0
                                && plan.getDateFinReelle() == null) {
                        plan.setDateFinReelle(LocalDate.now());
                        datesModifiees = true;
                }

                if (valeur.compareTo(new java.math.BigDecimal("100")) < 0
                                && plan.getDateFinReelle() != null) {
                        plan.setDateFinReelle(null);
                        datesModifiees = true;
                }

                if (datesModifiees) {
                        planActionRepository.save(plan);
                }

                // 5. Inserer la nouvelle ligne d'avancement
                AvancementPlanAction avancement = avancementPlanActionRepository.save(
                                AvancementPlanAction.builder()
                                                .valeurPourcentage(valeur)
                                                .commentaire(videSiBlanc(request.getCommentaire()))
                                                .dateChangement(LocalDateTime.now())
                                                .utilisateur(utilisateur)
                                                .statut(statutNouveau)
                                                .planAction(plan)
                                                .build());

                // 6. Renvoyer le DTO
                return AvancementPlanActionDetailDTO.builder()
                                .id(avancement.getIdAvancementPlanAction())
                                .valeurPourcentage(avancement.getValeurPourcentage())
                                .commentaire(avancement.getCommentaire())
                                .dateChangement(avancement.getDateChangement())
                                .statut(ReferenceDTO.builder()
                                                .id(statutNouveau.getIdStatut())
                                                .code(statutNouveau.getCode())
                                                .libelle(statutNouveau.getLibelle())
                                                .build())
                                .utilisateur(ReferenceDTO.builder()
                                                .id(utilisateur.getIdUtilisateur())
                                                .libelle(utilisateur.getPrenom() + " " + utilisateur.getNom())
                                                .build())
                                .courant(true)
                                .build();
        }

        // ------------------------------------------------------------------
        // Helpers avancement
        // ------------------------------------------------------------------

        private void verifierRoleSaisie(Integer idPlanAction, Integer idUtilisateur) {
                boolean estResponsable = affectationPlanActionRepository
                                .findHistorique(idPlanAction)
                                .stream()
                                .anyMatch(a -> a.getDateDesaffectation() == null
                                                && ROLES_SAISIE.contains(a.getRoleCode())
                                                && a.getUtilisateurId().equals(idUtilisateur));

                if (!estResponsable) {
                        throw new ApiException(
                                        "Seul le responsable ou le suppléant du plan peut saisir un avancement.",
                                        HttpStatus.FORBIDDEN);
                }
        }

        private String calculerStatut(java.math.BigDecimal valeur) {
                if (valeur.compareTo(java.math.BigDecimal.ZERO) == 0) {
                        return "NON_COMMENCEE";
                }
                if (valeur.compareTo(new java.math.BigDecimal("100")) == 0) {
                        return "TERMINEE";
                }
                return "EN_COURS";
        }

        private AvancementPlanActionDetailDTO versAvancementDetail(
                        AvancementPlanActionRow row, boolean courant) {
                return AvancementPlanActionDetailDTO.builder()
                                .id(row.getId())
                                .valeurPourcentage(row.getValeurPourcentage())
                                .commentaire(row.getCommentaire())
                                .dateChangement(row.getDateChangement())
                                .statut(row.getStatutId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getStatutId())
                                                                .code(row.getStatutCode())
                                                                .libelle(row.getStatutLibelle())
                                                                .build())
                                .utilisateur(row.getUtilisateurId() == null ? null
                                                : ReferenceDTO.builder()
                                                                .id(row.getUtilisateurId())
                                                                .libelle(row.getUtilisateurPrenom() + " "
                                                                                + row.getUtilisateurNom())
                                                                .build())
                                .courant(courant)
                                .build();
        }

        private String videSiBlanc(String valeur) {
                return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
        }
}