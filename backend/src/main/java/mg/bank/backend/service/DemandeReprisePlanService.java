package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.DecisionDemandeRequest;
import mg.bank.backend.dto.DemandeRepriseRequest;
import mg.bank.backend.dto.ReferenceDTO;
import mg.bank.backend.dto.planaction.DemandeReprisePlanDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.AffectationPlanAction;
import mg.bank.backend.model.DemandeReprisePlanAction;
import mg.bank.backend.model.Notification;
import mg.bank.backend.model.PlanAction;
import mg.bank.backend.model.Priorite;
import mg.bank.backend.model.Role;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.AffectationPlanActionRepository;
import mg.bank.backend.repository.AvancementPlanActionRepository;
import mg.bank.backend.repository.DemandeReprisePlanActionRepository;
import mg.bank.backend.repository.NotificationRepository;
import mg.bank.backend.repository.PlanActionRepository;
import mg.bank.backend.repository.PrioriteRepository;
import mg.bank.backend.repository.RoleRepository;
import mg.bank.backend.repository.UtilisateurRepository;
import mg.bank.backend.repository.projection.AffectationPlanActionRow;
import mg.bank.backend.repository.projection.AvancementPlanActionRow;
import mg.bank.backend.repository.projection.DemandeReprisePlanRow;

/**
 * Demandes de reprise d'un plan d'action NON_COMMENCEE.
 *
 * <p>Le plan n'a pas de service propre : le perimetre est celui du
 * RESPONSABLE actuel (convention du trigger
 * verifier_notifications_plans_action).</p>
 *
 * <p>Regles :</p>
 * <ul>
 *   <li>demandeur actif, du meme service que le RESPONSABLE actuel ;</li>
 *   <li>ne peut pas etre le RESPONSABLE actuel lui-meme ;</li>
 *   <li>plan au statut NON_COMMENCEE ;</li>
 *   <li>une seule demande EN_ATTENTE par plan ;</li>
 *   <li>decision : RESPONSABLE actuel OU chef de service du RESPONSABLE ;</li>
 *   <li>acceptation : desaffecte SUPPLEANT et RESPONSABLE actuels, affecte
 *       le demandeur comme RESPONSABLE. Statut du plan inchange.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DemandeReprisePlanService {

    private static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    private static final String STATUT_ACCEPTEE = "ACCEPTEE";
    private static final String STATUT_REFUSEE = "REFUSEE";
    private static final String STATUT_ANNULEE = "ANNULEE";
    private static final String ROLE_RESPONSABLE = "RESPONSABLE";
    private static final String ROLE_SUPPLEANT = "SUPPLEANT";
    private static final String STATUT_PLAN_NON_COMMENCEE = "NON_COMMENCEE";
    private static final String RESOURCE = "DEMANDE_REPRISE_PLAN";

    private final DemandeReprisePlanActionRepository demandeRepository;
    private final PlanActionRepository planActionRepository;
    private final AffectationPlanActionRepository affectationRepository;
    private final AvancementPlanActionRepository avancementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;
    private final PrioriteRepository prioriteRepository;

    private final ActiviteService activiteService;

    // ------------------------------------------------------------------
    // F1 — Créer une demande
    // ------------------------------------------------------------------

    @Transactional
    public DemandeReprisePlanDTO creer(Integer idPlanAction, DemandeRepriseRequest request) {

        Utilisateur demandeur = activiteService.utilisateurCourant();

        if (!Boolean.TRUE.equals(demandeur.getActif())) {
            throw new ApiException(
                    "Votre compte n'est pas actif : aucune demande possible.",
                    HttpStatus.FORBIDDEN);
        }

        PlanAction plan = planActionRepository.findById(idPlanAction)
                .orElseThrow(() -> new ApiException(
                        "Plan d'action introuvable.", HttpStatus.NOT_FOUND));

        // Statut NON_COMMENCEE requis.
        verifierStatutNonCommencee(idPlanAction);

        // Responsable actif requis.
        AffectationPlanActionRow respActuel = trouverResponsableActif(idPlanAction);
        if (respActuel == null) {
            throw new ApiException(
                    "Le plan n'a pas de responsable actif : aucune reprise possible.",
                    HttpStatus.CONFLICT);
        }

        Utilisateur responsableActuel = utilisateurRepository
                .findById(respActuel.getUtilisateurId())
                .orElseThrow(() -> new ApiException(
                        "Responsable introuvable.", HttpStatus.INTERNAL_SERVER_ERROR));

        // Perimetre : demandeur du meme service que le RESPONSABLE actuel.
        if (responsableActuel.getService() == null) {
            throw new ApiException(
                    "Le responsable actuel n'a pas de service : aucune reprise possible.",
                    HttpStatus.CONFLICT);
        }
        if (demandeur.getService() == null
                || !demandeur.getService().getIdService()
                        .equals(responsableActuel.getService().getIdService())) {
            throw new ApiException(
                    "Vous n'appartenez pas au service du responsable du plan.",
                    HttpStatus.FORBIDDEN);
        }

        // Pas soi-même.
        if (respActuel.getUtilisateurId().equals(demandeur.getIdUtilisateur())) {
            throw new ApiException(
                    "Vous êtes déjà le responsable de ce plan d'action.",
                    HttpStatus.CONFLICT);
        }

        // Pas de doublon EN_ATTENTE.
        if (demandeRepository.existsByPlanActionIdPlanActionAndStatut(
                idPlanAction, STATUT_EN_ATTENTE)) {
            throw new ApiException(
                    "Une demande de reprise est déjà en attente sur ce plan d'action.",
                    HttpStatus.CONFLICT);
        }

        DemandeReprisePlanAction demande = demandeRepository.save(
                DemandeReprisePlanAction.builder()
                        .dateDemande(LocalDateTime.now())
                        .statut(STATUT_EN_ATTENTE)
                        .motif(videSiBlanc(request == null ? null : request.getMotif()))
                        .planAction(plan)
                        .demandeur(demandeur)
                        .responsableActuel(responsableActuel)
                        .build());

        notifier(responsableActuel, demande,
                "Nouvelle demande de reprise — plan d'action « " + plan.getCode() + " »",
                "L'utilisateur " + libelle(demandeur)
                        + " souhaite reprendre le plan d'action « "
                        + plan.getCode() + " — " + plan.getDesignation() + " »."
                        + (demande.getMotif() != null ? " Motif : " + demande.getMotif() : ""));

        return versDto(demande);
    }

    // ------------------------------------------------------------------
    // F2 — Historique par plan
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DemandeReprisePlanDTO> historiquePlan(Integer idPlanAction) {
        if (!planActionRepository.existsById(idPlanAction)) {
            throw new ApiException("Plan d'action introuvable.", HttpStatus.NOT_FOUND);
        }
        return demandeRepository.findHistoriquePlan(idPlanAction)
                .stream().map(this::versDto).toList();
    }

    // ------------------------------------------------------------------
    // F3 / F4 — Mes demandes / à traiter
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DemandeReprisePlanDTO> mesDemandes() {
        Utilisateur utilisateur = activiteService.utilisateurCourant();
        return demandeRepository.findMesDemandes(utilisateur.getIdUtilisateur())
                .stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeReprisePlanDTO> aTraiter() {
        Utilisateur utilisateur = activiteService.utilisateurCourant();
        return demandeRepository.findATraiter(utilisateur.getIdUtilisateur())
                .stream().map(this::versDto).toList();
    }

    // ------------------------------------------------------------------
    // F5 — Accepter
    // ------------------------------------------------------------------

    @Transactional
    public DemandeReprisePlanDTO accepter(Integer idDemande, DecisionDemandeRequest request) {
        DemandeReprisePlanAction demande = chargerDemandeEnAttente(idDemande);
        Utilisateur decideur = activiteService.utilisateurCourant();

        verifierDroitDeDecision(demande, decideur);

        // Revalider l'etat.
        verifierStatutNonCommencee(demande.getPlanAction().getIdPlanAction());

        if (!Boolean.TRUE.equals(demande.getDemandeur().getActif())) {
            throw new ApiException(
                    "Le demandeur n'est plus actif : la demande ne peut pas être acceptée.",
                    HttpStatus.CONFLICT);
        }

        Utilisateur resp = demande.getResponsableActuel();
        if (resp.getService() == null
                || demande.getDemandeur().getService() == null
                || !demande.getDemandeur().getService().getIdService()
                        .equals(resp.getService().getIdService())) {
            throw new ApiException(
                    "Le demandeur n'appartient plus au service du responsable.",
                    HttpStatus.CONFLICT);
        }

        // Desaffecter SUPPLEANT puis RESPONSABLE actuels.
        desaffecterRole(demande.getPlanAction().getIdPlanAction(), ROLE_SUPPLEANT);
        desaffecterRole(demande.getPlanAction().getIdPlanAction(), ROLE_RESPONSABLE);

        // Affecter le demandeur comme RESPONSABLE.
        Role roleResponsable = roleRepository.findByCodeIgnoreCase(ROLE_RESPONSABLE)
                .orElseThrow(() -> new ApiException(
                        "Rôle RESPONSABLE introuvable en base.",
                        HttpStatus.INTERNAL_SERVER_ERROR));

        affectationRepository.save(AffectationPlanAction.builder()
                .dateAffectation(LocalDateTime.now())
                .role(roleResponsable)
                .planAction(demande.getPlanAction())
                .utilisateur(demande.getDemandeur())
                .build());

        demande.setStatut(STATUT_ACCEPTEE);
        demande.setDateDecision(LocalDateTime.now());
        demande.setDecideur(decideur);
        demande.setCommentaireDecision(
                videSiBlanc(request == null ? null : request.getCommentaire()));
        demandeRepository.save(demande);

        notifier(demande.getDemandeur(), demande,
                "Votre demande de reprise a été acceptée",
                "Vous êtes désormais responsable du plan d'action « "
                        + demande.getPlanAction().getCode() + " — "
                        + demande.getPlanAction().getDesignation() + " »."
                        + (demande.getCommentaireDecision() != null
                                ? " Commentaire : " + demande.getCommentaireDecision() : ""));

        return versDto(demande);
    }

    // ------------------------------------------------------------------
    // F6 — Refuser
    // ------------------------------------------------------------------

    @Transactional
    public DemandeReprisePlanDTO refuser(Integer idDemande, DecisionDemandeRequest request) {
        DemandeReprisePlanAction demande = chargerDemandeEnAttente(idDemande);
        Utilisateur decideur = activiteService.utilisateurCourant();

        verifierDroitDeDecision(demande, decideur);

        demande.setStatut(STATUT_REFUSEE);
        demande.setDateDecision(LocalDateTime.now());
        demande.setDecideur(decideur);
        demande.setCommentaireDecision(
                videSiBlanc(request == null ? null : request.getCommentaire()));
        demandeRepository.save(demande);

        notifier(demande.getDemandeur(), demande,
                "Votre demande de reprise a été refusée",
                "Votre demande sur le plan d'action « "
                        + demande.getPlanAction().getCode() + " — "
                        + demande.getPlanAction().getDesignation() + " » a été refusée."
                        + (demande.getCommentaireDecision() != null
                                ? " Motif : " + demande.getCommentaireDecision() : ""));

        return versDto(demande);
    }

    // ------------------------------------------------------------------
    // F7 — Annuler
    // ------------------------------------------------------------------

    @Transactional
    public DemandeReprisePlanDTO annuler(Integer idDemande) {
        DemandeReprisePlanAction demande = chargerDemandeEnAttente(idDemande);
        Utilisateur demandeur = activiteService.utilisateurCourant();

        if (!demande.getDemandeur().getIdUtilisateur()
                .equals(demandeur.getIdUtilisateur())) {
            throw new ApiException(
                    "Seul le demandeur peut annuler sa propre demande.",
                    HttpStatus.FORBIDDEN);
        }

        demande.setStatut(STATUT_ANNULEE);
        demande.setDateDecision(LocalDateTime.now());
        demande.setDecideur(demandeur);
        demandeRepository.save(demande);

        notifier(demande.getResponsableActuel(), demande,
                "Demande de reprise annulée",
                "La demande de reprise sur le plan d'action « "
                        + demande.getPlanAction().getCode() + " — "
                        + demande.getPlanAction().getDesignation()
                        + " » a été annulée par son auteur.");

        return versDto(demande);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private DemandeReprisePlanAction chargerDemandeEnAttente(Integer idDemande) {
        return demandeRepository
                .findByIdDemandeReprisePlanActionAndStatut(idDemande, STATUT_EN_ATTENTE)
                .orElseThrow(() -> new ApiException(
                        "Demande introuvable ou déjà traitée.",
                        HttpStatus.NOT_FOUND));
    }

    private void verifierStatutNonCommencee(Integer idPlanAction) {
        AvancementPlanActionRow dernier = avancementRepository
                .findDernier(idPlanAction)
                .orElseThrow(() -> new ApiException(
                        "Aucun avancement enregistré pour ce plan.",
                        HttpStatus.CONFLICT));

        if (!STATUT_PLAN_NON_COMMENCEE.equals(dernier.getStatutCode())) {
            throw new ApiException(
                    "Seul un plan d'action non commencé peut faire l'objet d'une reprise.",
                    HttpStatus.CONFLICT);
        }
    }

    private AffectationPlanActionRow trouverResponsableActif(Integer idPlanAction) {
        return affectationRepository.findHistorique(idPlanAction).stream()
                .filter(a -> a.getDateDesaffectation() == null)
                .filter(a -> ROLE_RESPONSABLE.equals(a.getRoleCode()))
                .findFirst()
                .orElse(null);
    }

    private void desaffecterRole(Integer idPlanAction, String codeRole) {
        affectationRepository.findHistorique(idPlanAction).stream()
                .filter(a -> a.getDateDesaffectation() == null)
                .filter(a -> codeRole.equals(a.getRoleCode()))
                .forEach(a -> affectationRepository.findById(a.getId()).ifPresent(ent -> {
                    ent.setDateDesaffectation(LocalDateTime.now());
                    affectationRepository.save(ent);
                }));
    }

    private void verifierDroitDeDecision(DemandeReprisePlanAction demande, Utilisateur decideur) {
        boolean estResponsableActuel = demande.getResponsableActuel().getIdUtilisateur()
                .equals(decideur.getIdUtilisateur());

        Utilisateur resp = demande.getResponsableActuel();
        boolean estChefDeService = decideur.getPoste() != null
                && decideur.getPoste().getLibelle() != null
                && decideur.getPoste().getLibelle().toLowerCase().startsWith("chef de service")
                && Boolean.TRUE.equals(decideur.getActif())
                && decideur.getService() != null
                && resp.getService() != null
                && decideur.getService().getIdService()
                        .equals(resp.getService().getIdService());

        if (!estResponsableActuel && !estChefDeService) {
            throw new ApiException(
                    "Seul le responsable actuel ou le chef de service du responsable "
                            + "peut décider.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void notifier(Utilisateur destinataire, DemandeReprisePlanAction demande,
                          String titre, String message) {

        Priorite priorite = prioriteRepository.findByCodeIgnoreCase("HAUTE")
                .filter(Priorite::getActif)
                .orElseGet(() -> prioriteRepository.findAll().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActif()))
                        .findFirst()
                        .orElse(null));

        if (priorite == null) {
            return;
        }

        notificationRepository.save(Notification.builder()
                .titre(titre)
                .message(message)
                .dateCreation(LocalDateTime.now())
                .priorite(priorite)
                .utilisateur(destinataire)
                .idReference(demande.getIdDemandeReprisePlanAction())
                .resource(RESOURCE)
                .build());
    }

    private DemandeReprisePlanDTO versDto(DemandeReprisePlanAction d) {
        return DemandeReprisePlanDTO.builder()
                .id(d.getIdDemandeReprisePlanAction())
                .dateDemande(d.getDateDemande())
                .dateDecision(d.getDateDecision())
                .statut(d.getStatut())
                .motif(d.getMotif())
                .commentaireDecision(d.getCommentaireDecision())
                .planAction(refPlan(d.getPlanAction()))
                .demandeur(refUtilisateur(d.getDemandeur()))
                .responsableActuel(refUtilisateur(d.getResponsableActuel()))
                .decideur(refUtilisateur(d.getDecideur()))
                .build();
    }

    private DemandeReprisePlanDTO versDto(DemandeReprisePlanRow r) {
        return DemandeReprisePlanDTO.builder()
                .id(r.getId())
                .dateDemande(r.getDateDemande())
                .dateDecision(r.getDateDecision())
                .statut(r.getStatut())
                .motif(r.getMotif())
                .commentaireDecision(r.getCommentaireDecision())
                .planAction(ReferenceDTO.builder()
                        .id(r.getPlanActionId())
                        .code(r.getPlanActionCode())
                        .libelle(r.getPlanActionDesignation())
                        .build())
                .demandeur(ReferenceDTO.builder()
                        .id(r.getDemandeurId())
                        .libelle(r.getDemandeurPrenom() + " " + r.getDemandeurNom())
                        .build())
                .responsableActuel(ReferenceDTO.builder()
                        .id(r.getResponsableId())
                        .libelle(r.getResponsablePrenom() + " " + r.getResponsableNom())
                        .build())
                .decideur(r.getDecideurId() == null ? null
                        : ReferenceDTO.builder()
                                .id(r.getDecideurId())
                                .libelle(r.getDecideurPrenom() + " " + r.getDecideurNom())
                                .build())
                .build();
    }

    private ReferenceDTO refPlan(PlanAction p) {
        return p == null ? null : ReferenceDTO.builder()
                .id(p.getIdPlanAction())
                .code(p.getCode())
                .libelle(p.getDesignation())
                .build();
    }

    private ReferenceDTO refUtilisateur(Utilisateur u) {
        return u == null ? null : ReferenceDTO.builder()
                .id(u.getIdUtilisateur())
                .libelle(libelle(u))
                .build();
    }

    private String libelle(Utilisateur u) {
        return u.getPrenom() + " " + u.getNom();
    }

    private String videSiBlanc(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}