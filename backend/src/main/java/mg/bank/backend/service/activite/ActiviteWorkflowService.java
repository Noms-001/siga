package mg.bank.backend.service.activite;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.ActiviteTransitionResponse;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.referentiel.Statut;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.affectation.AffectationActiviteRepository;
import mg.bank.backend.repository.affectation.AffectationSousActiviteRepository;
import mg.bank.backend.repository.projection.common.StatutCourantRow;
import mg.bank.backend.repository.referentiel.StatutRepository;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@Service
@RequiredArgsConstructor
public class ActiviteWorkflowService {

        private final ActiviteRepository activiteRepository;
        private final AffectationActiviteRepository affectationActiviteRepository;
        private final AffectationSousActiviteRepository affectationSousActiviteRepository;
        private final StatutRepository statutRepository;
        private final ActiviteService activiteService;
        private final CurrentUserService currentUserService;

        // ------------------------------------------------------------------
        // Demarrage
        // ------------------------------------------------------------------

        @Transactional
        public ActiviteTransitionResponse demarrer(Integer idActivite, String commentaire) {
                validerId(idActivite);

                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
                Utilisateur utilisateur = currentUserService.get();

                Activite activite = activiteRepository.verrouillerActivite(idActivite)
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

                StatutCourantRow statut = statutCourant(idActivite, perimetre);

                // C1 : NON_COMMENCEE, pas VALIDEE
                if (!StatutsActivite.NON_COMMENCEE.equals(statut.getStatutCode())) {
                        throw new ApiException(
                                        "Une activité ne peut être démarrée que si elle est validée et non encore démarrée. "
                                                        + "Statut actuel : " + statut.getStatutCode(),
                                        HttpStatus.CONFLICT);
                }

                // C2 : RESPONSABLE sur l'activité principale UNIQUEMENT
                long responsablesSurActivite = affectationActiviteRepository
                                .countResponsableActif(idActivite);

                if (responsablesSurActivite == 0) {
                        throw new ApiException(
                                        "L'activité n'a aucun responsable actif sur l'activité principale. "
                                                        + "Les affectations sur les sous-activités ne comptent pas.",
                                        HttpStatus.CONFLICT);
                }

                LocalDate dateDebut = LocalDate.now();
                activite.setDateDebutReelle(dateDebut);
                activiteRepository.saveAndFlush(activite);

                Statut cible = statutCible(StatutsActivite.EN_COURS);
                activiteService.changerStatut(activite, cible, utilisateur,
                                commentaire("Démarrage de l'activité", commentaire));

                return response(activite, cible, dateDebut, null);
        }

        // ------------------------------------------------------------------
        // Cloture
        // ------------------------------------------------------------------

        @Transactional
        public ActiviteTransitionResponse cloturer(Integer idActivite, String commentaire) {

                validerId(idActivite);

                PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
                Utilisateur utilisateur = currentUserService.get();

                Activite activite = activiteRepository.verrouillerActivite(idActivite)
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

                StatutCourantRow statut = statutCourant(idActivite, perimetre);

                if (!StatutsActivite.EN_COURS.equals(statut.getStatutCode())) {
                        throw new ApiException(
                                        "Une activité ne peut être clôturée que si elle est en cours. "
                                                        + "Statut actuel : " + statut.getStatutCode(),
                                        HttpStatus.CONFLICT);
                }

                long nonTerminees = activiteRepository.compterSousActivitesNonTerminees(idActivite);
                if (nonTerminees > 0) {
                        throw new ApiException(
                                        "La clôture requiert que toutes les sous-activités soient terminées : "
                                                        + nonTerminees + " sous-activité(s) ne le sont pas.",
                                        HttpStatus.CONFLICT);
                }

                Double avancement = activiteRepository.findAvancementActivite(idActivite);
                if (avancement == null || avancement < 100d) {
                        throw new ApiException(
                                        "La clôture requiert un avancement global de 100 % : valeur actuelle "
                                                        + (avancement == null ? 0 : avancement) + " %.",
                                        HttpStatus.CONFLICT);
                }

                LocalDate dateFin = LocalDate.now();
                activite.setDateFinReelle(dateFin);
                activiteRepository.saveAndFlush(activite);

                Statut cible = statutCible(StatutsActivite.TERMINEE);
                activiteService.changerStatut(
                                activite,
                                cible,
                                utilisateur,
                                commentaire("Clôture de l'activité", commentaire));

                return response(activite, cible, null, dateFin);
        }

        // ------------------------------------------------------------------
        // Helpers
        // ------------------------------------------------------------------

        private StatutCourantRow statutCourant(Integer idActivite, PerimetreUtilisateur perimetre) {
                return activiteRepository.findStatutCourant(
                                idActivite,
                                perimetre.getFiltreService(),
                                perimetre.getFiltreDepartement())
                                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));
        }

        private Statut statutCible(String code) {
                return statutRepository.findByCode(code)
                                .orElseThrow(() -> new ApiException(
                                                "Statut " + code + " introuvable dans le référentiel",
                                                HttpStatus.INTERNAL_SERVER_ERROR));
        }

        private String commentaire(String prefixe, String complement) {
                if (complement == null || complement.isBlank()) {
                        return prefixe;
                }
                return prefixe + " : " + complement.trim();
        }

        private void validerId(Integer idActivite) {
                if (idActivite == null || idActivite < 1) {
                        throw new ApiException(
                                        "L'identifiant de l'activité doit être un entier positif",
                                        HttpStatus.BAD_REQUEST);
                }
        }

        private ActiviteTransitionResponse response(
                        Activite activite, Statut statut, LocalDate dateDebut, LocalDate dateFin) {
                return ActiviteTransitionResponse.builder()
                                .idActivite(activite.getIdActivite())
                                .code(activite.getCode())
                                .designation(activite.getDesignation())
                                .statutCode(statut.getCode())
                                .statutLibelle(statut.getLibelle())
                                .dateDebutReelle(dateDebut != null ? dateDebut : activite.getDateDebutReelle())
                                .dateFinReelle(dateFin != null ? dateFin : activite.getDateFinReelle())
                                .dateTransition(LocalDateTime.now())
                                .build();
        }
}