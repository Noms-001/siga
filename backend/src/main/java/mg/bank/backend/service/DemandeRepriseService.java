package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ReferenceDTO;
import mg.bank.backend.dto.DecisionDemandeRequest;
import mg.bank.backend.dto.DemandeRepriseDTO;
import mg.bank.backend.dto.DemandeRepriseRequest;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Activite;
import mg.bank.backend.model.AffectationActivite;
import mg.bank.backend.model.DemandeRepriseActivite;
import mg.bank.backend.model.Notification;
import mg.bank.backend.model.Priorite;
import mg.bank.backend.model.Role;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.AffectationActiviteRepository;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.DemandeRepriseActiviteRepository;
import mg.bank.backend.repository.NotificationRepository;
import mg.bank.backend.repository.PrioriteRepository;
import mg.bank.backend.repository.RoleRepository;
import mg.bank.backend.repository.UtilisateurRepository;
import mg.bank.backend.repository.projection.AffectationRow;
import mg.bank.backend.repository.projection.DemandeRepriseRow;
import mg.bank.backend.repository.projection.StatutCourantRow;

@Service
@RequiredArgsConstructor
public class DemandeRepriseService {

    private static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    private static final String STATUT_ACCEPTEE = "ACCEPTEE";
    private static final String STATUT_REFUSEE = "REFUSEE";
    private static final String STATUT_ANNULEE = "ANNULEE";
    private static final String ROLE_RESPONSABLE = "RESPONSABLE";
    private static final String ROLE_SUPPLEANT = "SUPPLEANT";
    private static final String STATUT_ACTIVITE_NON_COMMENCEE = "NON_COMMENCEE";
    private static final String RESOURCE = "DEMANDE_REPRISE";

    private final DemandeRepriseActiviteRepository demandeRepository;
    private final ActiviteRepository activiteRepository;
    private final AffectationActiviteRepository affectationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;
    private final PrioriteRepository prioriteRepository;

    private final ActiviteService activiteService;

    // ------------------------------------------------------------------
    // F1 — Créer une demande
    // ------------------------------------------------------------------

    @Transactional
    public DemandeRepriseDTO creer(Integer idActivite, DemandeRepriseRequest request) {

        Utilisateur demandeur = activiteService.utilisateurCourant();

        if (!Boolean.TRUE.equals(demandeur.getActif())) {
            throw new ApiException(
                    "Votre compte n'est pas actif : aucune demande possible.",
                    HttpStatus.FORBIDDEN);
        }

        Activite activite = activiteRepository.findById(idActivite)
                .orElseThrow(() -> new ApiException(
                        "Activité introuvable.", HttpStatus.NOT_FOUND));

        // Perimetre : utilisateur du service de l'activite.
        if (demandeur.getService() == null || activite.getService() == null
                || !demandeur.getService().getIdService()
                        .equals(activite.getService().getIdService())) {
            throw new ApiException(
                    "Vous n'appartenez pas au service de cette activité.",
                    HttpStatus.FORBIDDEN);
        }

        // Statut NON_COMMENCEE requis.
        verifierStatutNonCommencee(idActivite);

        // Responsable actif requis.
        AffectationRow respActuel = trouverResponsableActif(idActivite);
        if (respActuel == null) {
            throw new ApiException(
                    "L'activité n'a pas de responsable actif : aucune reprise possible.",
                    HttpStatus.CONFLICT);
        }

        // Pas soi-même.
        if (respActuel.getUtilisateurId().equals(demandeur.getIdUtilisateur())) {
            throw new ApiException(
                    "Vous êtes déjà le responsable de cette activité.",
                    HttpStatus.CONFLICT);
        }

        // Pas de doublon EN_ATTENTE.
        if (demandeRepository.existsByActiviteIdActiviteAndStatut(
                idActivite, STATUT_EN_ATTENTE)) {
            throw new ApiException(
                    "Une demande de reprise est déjà en attente sur cette activité.",
                    HttpStatus.CONFLICT);
        }

        Utilisateur responsableActuel = utilisateurRepository
                .findById(respActuel.getUtilisateurId())
                .orElseThrow(() -> new ApiException(
                        "Responsable introuvable.", HttpStatus.INTERNAL_SERVER_ERROR));

        DemandeRepriseActivite demande = demandeRepository.save(
                DemandeRepriseActivite.builder()
                        .dateDemande(LocalDateTime.now())
                        .statut(STATUT_EN_ATTENTE)
                        .motif(videSiBlanc(request == null ? null : request.getMotif()))
                        .activite(activite)
                        .demandeur(demandeur)
                        .responsableActuel(responsableActuel)
                        .build());

        // Notification : RESPONSABLE actuel.
        notifier(responsableActuel, demande,
                "Nouvelle demande de reprise — activité « " + activite.getCode() + " »",
                "L'utilisateur " + libelle(demandeur)
                        + " souhaite reprendre l'activité « "
                        + activite.getCode() + " — " + activite.getDesignation() + " »."
                        + (demande.getMotif() != null ? " Motif : " + demande.getMotif() : ""));

        return versDto(demande);
    }

    // ------------------------------------------------------------------
    // F2 — Historique par activite
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DemandeRepriseDTO> historiqueActivite(Integer idActivite) {
        if (!activiteRepository.existsById(idActivite)) {
            throw new ApiException("Activité introuvable.", HttpStatus.NOT_FOUND);
        }
        return demandeRepository.findHistoriqueActivite(idActivite)
                .stream().map(this::versDto).toList();
    }

    // ------------------------------------------------------------------
    // F3 / F4 — Mes demandes / à traiter
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DemandeRepriseDTO> mesDemandes() {
        Utilisateur utilisateur = activiteService.utilisateurCourant();
        return demandeRepository.findMesDemandes(utilisateur.getIdUtilisateur())
                .stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeRepriseDTO> aTraiter() {
        Utilisateur utilisateur = activiteService.utilisateurCourant();
        return demandeRepository.findATraiter(utilisateur.getIdUtilisateur())
                .stream().map(this::versDto).toList();
    }

    // ------------------------------------------------------------------
    // F5 — Accepter
    // ------------------------------------------------------------------

    @Transactional
    public DemandeRepriseDTO accepter(Integer idDemande, DecisionDemandeRequest request) {
        DemandeRepriseActivite demande = chargerDemandeEnAttente(idDemande);
        Utilisateur decideur = activiteService.utilisateurCourant();

        verifierDroitDeDecision(demande, decideur);

        // Revalider que la situation n'a pas change.
        verifierStatutNonCommencee(demande.getActivite().getIdActivite());

        if (!Boolean.TRUE.equals(demande.getDemandeur().getActif())) {
            throw new ApiException(
                    "Le demandeur n'est plus actif : la demande ne peut pas être acceptée.",
                    HttpStatus.CONFLICT);
        }

        if (demande.getDemandeur().getService() == null
                || demande.getActivite().getService() == null
                || !demande.getDemandeur().getService().getIdService()
                        .equals(demande.getActivite().getService().getIdService())) {
            throw new ApiException(
                    "Le demandeur n'appartient plus au service de l'activité.",
                    HttpStatus.CONFLICT);
        }

        // Desaffecter SUPPLEANT puis RESPONSABLE actuels.
        desaffecterRole(demande.getActivite().getIdActivite(), ROLE_SUPPLEANT);
        desaffecterRole(demande.getActivite().getIdActivite(), ROLE_RESPONSABLE);

        // Affecter le demandeur comme RESPONSABLE.
        Role roleResponsable = roleRepository.findByCodeIgnoreCase(ROLE_RESPONSABLE)
                .orElseThrow(() -> new ApiException(
                        "Rôle RESPONSABLE introuvable en base.",
                        HttpStatus.INTERNAL_SERVER_ERROR));

        affectationRepository.save(AffectationActivite.builder()
                .dateAffectation(LocalDateTime.now())
                .role(roleResponsable)
                .activite(demande.getActivite())
                .utilisateur(demande.getDemandeur())
                .build());

        // Clore la demande.
        demande.setStatut(STATUT_ACCEPTEE);
        demande.setDateDecision(LocalDateTime.now());
        demande.setDecideur(decideur);
        demande.setCommentaireDecision(
                videSiBlanc(request == null ? null : request.getCommentaire()));
        demandeRepository.save(demande);

        notifier(demande.getDemandeur(), demande,
                "Votre demande de reprise a été acceptée",
                "Vous êtes désormais responsable de l'activité « "
                        + demande.getActivite().getCode() + " — "
                        + demande.getActivite().getDesignation() + " »."
                        + (demande.getCommentaireDecision() != null
                                ? " Commentaire : " + demande.getCommentaireDecision() : ""));

        return versDto(demande);
    }

    // ------------------------------------------------------------------
    // F6 — Refuser
    // ------------------------------------------------------------------

    @Transactional
    public DemandeRepriseDTO refuser(Integer idDemande, DecisionDemandeRequest request) {
        DemandeRepriseActivite demande = chargerDemandeEnAttente(idDemande);
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
                "Votre demande sur l'activité « " + demande.getActivite().getCode()
                        + " — " + demande.getActivite().getDesignation() + " » a été refusée."
                        + (demande.getCommentaireDecision() != null
                                ? " Motif : " + demande.getCommentaireDecision() : ""));

        return versDto(demande);
    }

    // ------------------------------------------------------------------
    // F7 — Annuler
    // ------------------------------------------------------------------

    @Transactional
    public DemandeRepriseDTO annuler(Integer idDemande) {
        DemandeRepriseActivite demande = chargerDemandeEnAttente(idDemande);
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
                "La demande de reprise sur l'activité « "
                        + demande.getActivite().getCode() + " — "
                        + demande.getActivite().getDesignation()
                        + " » a été annulée par son auteur.");

        return versDto(demande);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private DemandeRepriseActivite chargerDemandeEnAttente(Integer idDemande) {
        return demandeRepository
                .findByIdDemandeRepriseActiviteAndStatut(idDemande, STATUT_EN_ATTENTE)
                .orElseThrow(() -> new ApiException(
                        "Demande introuvable ou déjà traitée.",
                        HttpStatus.NOT_FOUND));
    }

    private void verifierStatutNonCommencee(Integer idActivite) {
        StatutCourantRow statut = activiteRepository.findStatutCourant(
                        idActivite, null, null)
                .orElseThrow(() -> new ApiException(
                        "Activité introuvable.", HttpStatus.NOT_FOUND));
        if (!STATUT_ACTIVITE_NON_COMMENCEE.equals(statut.getStatutCode())) {
            throw new ApiException(
                    "Seule une activité non commencée peut faire l'objet d'une reprise.",
                    HttpStatus.CONFLICT);
        }
    }

    private AffectationRow trouverResponsableActif(Integer idActivite) {
        return affectationRepository.findHistorique(idActivite).stream()
                .filter(a -> a.getDateDesaffectation() == null)
                .filter(a -> ROLE_RESPONSABLE.equals(a.getRoleCode()))
                .findFirst()
                .orElse(null);
    }

    private void desaffecterRole(Integer idActivite, String codeRole) {
        affectationRepository.findHistorique(idActivite).stream()
                .filter(a -> a.getDateDesaffectation() == null)
                .filter(a -> codeRole.equals(a.getRoleCode()))
                .forEach(a -> affectationRepository.findById(a.getId()).ifPresent(ent -> {
                    ent.setDateDesaffectation(LocalDateTime.now());
                    affectationRepository.save(ent);
                }));
    }

    private void verifierDroitDeDecision(DemandeRepriseActivite demande, Utilisateur decideur) {
        boolean estResponsableActuel = demande.getResponsableActuel().getIdUtilisateur()
                .equals(decideur.getIdUtilisateur());

        boolean estChefDeService = decideur.getPoste() != null
                && decideur.getPoste().getLibelle() != null
                && decideur.getPoste().getLibelle().toLowerCase().startsWith("chef de service")
                && Boolean.TRUE.equals(decideur.getActif())
                && decideur.getService() != null
                && demande.getActivite().getService() != null
                && decideur.getService().getIdService()
                        .equals(demande.getActivite().getService().getIdService());

        if (!estResponsableActuel && !estChefDeService) {
            throw new ApiException(
                    "Seul le responsable actuel ou le chef de service peut décider.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void notifier(Utilisateur destinataire, DemandeRepriseActivite demande,
                          String titre, String message) {

        Priorite priorite = prioriteRepository.findByCodeIgnoreCase("HAUTE")
                .filter(Priorite::getActif)
                .orElseGet(() -> prioriteRepository.findAll().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActif()))
                        .findFirst()
                        .orElse(null));

        if (priorite == null) {
            // Aucune priorite active : on renonce a la notification sans faire
            // echouer l'operation metier.
            return;
        }

        notificationRepository.save(Notification.builder()
                .titre(titre)
                .message(message)
                .dateCreation(LocalDateTime.now())
                .priorite(priorite)
                .utilisateur(destinataire)
                .idReference(demande.getIdDemandeRepriseActivite())
                .resource(RESOURCE)
                .build());
    }

    private DemandeRepriseDTO versDto(DemandeRepriseActivite d) {
        return DemandeRepriseDTO.builder()
                .id(d.getIdDemandeRepriseActivite())
                .dateDemande(d.getDateDemande())
                .dateDecision(d.getDateDecision())
                .statut(d.getStatut())
                .motif(d.getMotif())
                .commentaireDecision(d.getCommentaireDecision())
                .activite(refActivite(d.getActivite()))
                .demandeur(refUtilisateur(d.getDemandeur()))
                .responsableActuel(refUtilisateur(d.getResponsableActuel()))
                .decideur(refUtilisateur(d.getDecideur()))
                .build();
    }

    private DemandeRepriseDTO versDto(DemandeRepriseRow r) {
        return DemandeRepriseDTO.builder()
                .id(r.getId())
                .dateDemande(r.getDateDemande())
                .dateDecision(r.getDateDecision())
                .statut(r.getStatut())
                .motif(r.getMotif())
                .commentaireDecision(r.getCommentaireDecision())
                .activite(ReferenceDTO.builder()
                        .id(r.getActiviteId())
                        .code(r.getActiviteCode())
                        .libelle(r.getActiviteDesignation())
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

    private ReferenceDTO refActivite(Activite a) {
        return a == null ? null : ReferenceDTO.builder()
                .id(a.getIdActivite())
                .code(a.getCode())
                .libelle(a.getDesignation())
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