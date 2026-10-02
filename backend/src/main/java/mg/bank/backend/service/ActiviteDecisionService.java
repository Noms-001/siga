package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ActiviteDecisionDTO;
import mg.bank.backend.dto.ActiviteDecisionRequest;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.enums.DecisionValidation;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Activite;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.projection.StatutCourantRow;

/**
 * Decision de validation : valider, rejeter, ou retourner pour modification.
 *
 * UNE DECISION ECRIT DANS DEUX TABLES, ET C'EST VOLONTAIRE
 * La machine a statuts (historique_activite) dit ou en est l activite ; le
 * circuit de validation (validation_activite) dit qui l a examinee, a quelle
 * etape et pourquoi. Une seule des deux suffirait a afficher une liste, mais
 * elle perdrait l explication, et une decision sans statut ne sortirait pas de
 * la page. Les deux ecritures sont dans la meme transaction : une decision
 * prise sans effet visible, ou un statut change sans decision, seraient deux
 * etats impossibles a voir.
 *
 * LES TROIS DECISIONS ET LEUR STATUT
 * VALIDE devient VALIDEE, l activite est publiee au suivi. REJETE devient
 * REJETE, elle en sort definitivement. RETOUR_MODIFICATION redevient BROUILLON :
 * c est la seule des trois qui rende l activite modifiable, puisque seule une
 * activite en brouillon peut etre modifiee puis resoumise.
 *
 * CE QUE CE SERVICE NE FAIT PAS
 * Il n enchaîne pas les niveaux du circuit : une decision tranche l examen et
 * clot le passage. Le suivi de plusieurs niveaux est une evolution, pas une
 * coincidence a eviter ici.
 *
 * LE VERROU EST PRIS AVANT LA RELECTURE DU STATUT
 * Comme pour la soumission : deux onglets qui decident au meme moment ne
 * peuvent pas tous deux lire EN_ATTENTE_VALIDATION et ecrire deux decisions.
 * Le second est refuse en conflit, ce qui est exact : la decision existe deja.
 */
@Service
@RequiredArgsConstructor
public class ActiviteDecisionService {

    private final ActiviteRepository activiteRepository;

    private final ActiviteService activiteService;

    /**
     * Trancher l examen d une activite soumise.
     *
     * 400 decision inconnue ou refus sans motif, 404 activite inexistante, 403
     * hors perimetre, 409 si elle n attend plus de decision -- deux onglets
     * ouverts, ou une decision deja prise.
     */
    @Transactional
    public ActiviteDecisionDTO decider(Integer idActivite, ActiviteDecisionRequest request) {

        if (idActivite == null || idActivite < 1) {
            throw new ApiException(
                    "L identifiant de l activite doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        DecisionValidation decision = decision(request);
        String commentaire = request.commentaireNettoye();

        if (request.commentaireObligatoire()
                && (commentaire == null || commentaire.isBlank())) {
            throw new ApiException(
                    "Un refus doit etre motive : indiquez ce qui doit etre corrige",
                    HttpStatus.BAD_REQUEST);
        }

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        Utilisateur decideur = activiteService.utilisateurCourant();

        Activite verrouillee = activiteRepository.verrouillerActivite(idActivite)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        StatutCourantRow courant = statutCourant(idActivite, perimetre)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        if (!StatutsActivite.EN_ATTENTE_VALIDATION.equals(courant.getStatutCode())) {
            throw new ApiException(
                    "Seule une activite en attente de validation peut faire l objet d une decision",
                    HttpStatus.CONFLICT);
        }

        /*
            Aucune demande ouverte n est signalee comme un conflit et non comme
            un succes : l activite est bien en attente d etat, mais le circuit
            ne la connait pas, et y ecrire une decision laisserait un examen sans
            demande. C est le cas d une activite soumise avant que la soumission
            n enregistre sa demande, et il vaut mieux le dire que le deviner.
        */
        if (activiteRepository.enregistrerDecision(
                idActivite, decideur.getIdUtilisateur(), decision.name(), commentaire) == 0) {
            throw new ApiException(
                    "Cette activite n attend aucune decision de validation",
                    HttpStatus.CONFLICT);
        }

        String statut = statutPour(decision);

        activiteRepository.ajouterChangementStatut(
                idActivite,
                decideur.getIdUtilisateur(),
                statut,
                commentaireDecision(decision, commentaire));

        return ActiviteDecisionDTO.builder()
                .idActivite(verrouillee.getIdActivite())
                .code(verrouillee.getCode())
                .decision(decision.name())
                .statut(statut)
                .dateDecision(LocalDateTime.now())
                .build();
    }

    /**
     * Statut produit par une decision.
     *
     * La correspondance vit ici et nulle part ailleurs : c est la seule
     * endroit ou une decision de validation touche la machine a statuts.
     */
    private String statutPour(DecisionValidation decision) {
        return switch (decision) {
            case VALIDE -> StatutsActivite.VALIDEE;
            case REJETE -> StatutsActivite.REJETE;
            case RETOUR_MODIFICATION -> StatutsActivite.BROUILLON;
            case EN_ATTENTE_VALIDATION -> throw new ApiException(
                    "Une demande en attente n est pas une decision",
                    HttpStatus.BAD_REQUEST);
        };
    }

    /**
     * Commentaire porte par la ligne d historique.
     *
     * Un refus sans motif s'afficherait dans l historique avec le seul
     * mot de son type : c'est pourquoi le motif est obligatoire au moment de la
     * decision. Le commentaire du demandeur n'est pas repris ici : il
     * decrirait la soumission, pas l examen.
     */
    private String commentaireDecision(DecisionValidation decision, String commentaire) {
        String prefixe = switch (decision) {
            case VALIDE -> "Validee a la validation";
            case REJETE -> "Rejetee a la validation";
            case RETOUR_MODIFICATION -> "Retournee pour modification";
            case EN_ATTENTE_VALIDATION -> null;
        };

        // Un commentaire de plus de 1000 caracteres est refuse en amont par la
        // validation du corps, donc le prefixe ne peut pas faire deborder la
        // colonne : le total reste dans la limite annoncee au client.
        return commentaire == null || commentaire.isBlank()
                ? prefixe
                : prefixe + " : " + commentaire;
    }

    /**
     * Decision demandee, verifiee contre les trois issues possibles.
     *
     * La verification est explicite plutot qu'un valueOf : un corps envoye par
     * un client peut contenir n'importe quoi, et valueOf lèverait une
     * exception 500 sur un nom inconnu au lieu d'un refus lisible.
     */
    private DecisionValidation decision(ActiviteDecisionRequest request) {
        try {
            return DecisionValidation.valueOf(request.getDecision());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(
                    "La decision doit etre VALIDE, REJETE ou RETOUR_MODIFICATION",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Statut courant, restreint au perimetre.
     */
    private Optional<StatutCourantRow> statutCourant(
            Integer idActivite,
            PerimetreUtilisateur perimetre) {

        return activiteRepository.findStatutCourant(
                idActivite,
                perimetre.estRattacheService() ? perimetre.getIdService() : null,
                perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null);
    }

}
