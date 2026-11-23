package mg.bank.backend.service.activite;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.AvancementRequest;
import mg.bank.backend.dto.activite.AvancementResponse;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.activite.AvancementSousActivite;
import mg.bank.backend.model.activite.SousActivite;
import mg.bank.backend.model.referentiel.Statut;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.activite.AvancementSousActiviteRepository;
import mg.bank.backend.repository.activite.SousActiviteRepository;
import mg.bank.backend.repository.affectation.AffectationActiviteRepository;
import mg.bank.backend.repository.affectation.AffectationSousActiviteRepository;
import mg.bank.backend.service.common.PorteeService;
import mg.bank.backend.repository.projection.activite.AvancementSousActiviteRow;
import mg.bank.backend.repository.projection.common.StatutCourantRow;
import mg.bank.backend.repository.referentiel.StatutRepository;

@Service
@RequiredArgsConstructor
public class SousActiviteService {

        private final SousActiviteRepository sousActiviteRepository;
        private final AvancementSousActiviteRepository avancementRepository;
        private final StatutRepository statutRepository;
        private final ActiviteService activiteService;
        private final AffectationActiviteRepository affectationActiviteRepository;
        private final AffectationSousActiviteRepository affectationSousActiviteRepository;
        private final ActiviteRepository activiteRepository;
        private final PorteeService porteeService;

        @Transactional
        public AvancementResponse enregistrerAvancement(
                        Integer idSousActivite,
                        AvancementRequest request) {

                SousActivite sousActivite = sousActiviteRepository.findById(idSousActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Sous-activité introuvable",
                                                HttpStatus.NOT_FOUND));

                Activite activite = sousActivite.getActivite();
                porteeService.exigerActivite(activite.getIdActivite());
                Utilisateur utilisateur = activiteService.utilisateurCourant();

                verifierAvancementAutorise(
                                activite.getIdActivite(),
                                idSousActivite,
                                utilisateur);

                /*
                 * La dernière valeur est relue par la même requête que le batch de la
                 * liste : la valeur contre laquelle on compare ici est donc exactement
                 * celle que l'utilisateur a vue dans le modal. Une lecture séparée
                 * risquerait de diverger à cause d'un tri différent.
                 */
                Double ancienneValeur = avancementRepository
                                .findDerniersPourSousActivites(List.of(idSousActivite))
                                .stream()
                                .findFirst()
                                .map(AvancementSousActiviteRow::getValeur)
                                .orElse(null);

                int nouvelleValeur = request.getValeurPourcentage();

                // Contrôle de bornes : @Min/@Max sur le DTO le fait déjà pour un appel
                // HTTP, mais un appel Java direct contournerait la validation.
                if (nouvelleValeur < 0 || nouvelleValeur > 100) {
                        throw new ApiException(
                                        "L'avancement doit être compris entre 0 et 100",
                                        HttpStatus.BAD_REQUEST);
                }

                // Valeur identique : aucune modification à enregistrer. Le message est
                // celui affiché par le front dans le modal d'édition.
                if (ancienneValeur != null && ancienneValeur.intValue() == nouvelleValeur) {
                        throw new ApiException(
                                        "Aucune modification de l'avancement.",
                                        HttpStatus.BAD_REQUEST);
                }

                String commentaire = videSiBlanc(request.getCommentaire());

                if (ancienneValeur != null
                                && nouvelleValeur < ancienneValeur
                                && commentaire == null) {
                        throw new ApiException(
                                        "Une justification est obligatoire en cas de diminution "
                                                        + "de l'avancement.",
                                        HttpStatus.BAD_REQUEST);
                }

                Statut statut = statutPourAvancement(nouvelleValeur);

                AvancementSousActivite enregistrement = AvancementSousActivite.builder()
                                .sousActivite(sousActivite)
                                .utilisateur(utilisateur)
                                .statut(statut)
                                .valeurPourcentage(BigDecimal.valueOf(nouvelleValeur))
                                .commentaire(commentaire)
                                .dateChangement(LocalDateTime.now())
                                .build();

                avancementRepository.save(enregistrement);

                Double avancementActivite = activiteService
                                .calculerAvancementActivite(sousActivite.getActivite().getIdActivite());

                // Relecture du statut courant pour le renvoyer au front.
                String statutActiviteCode = activiteRepository
                                .findStatutCourant(activite.getIdActivite(), null, null, null)
                                .map(StatutCourantRow::getStatutCode)
                                .orElse(null);

                String statutActiviteLibelle = statutActiviteCode == null
                                ? null
                                : statutRepository.findByCode(statutActiviteCode)
                                                .map(Statut::getLibelle)
                                                .orElse(null);

                return AvancementResponse.builder()
                                .idSousActivite(idSousActivite)
                                .ancienneValeur(ancienneValeur)
                                .avancement((double) nouvelleValeur)
                                .commentaire(commentaire)
                                .dateChangement(enregistrement.getDateChangement())
                                .statutCode(statut.getCode())
                                .statutLibelle(statut.getLibelle())
                                .avancementActivite(avancementActivite)
                                .statutActiviteCode(statutActiviteCode) // ⬅️ AJOUTÉ
                                .statutActiviteLibelle(statutActiviteLibelle)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<AvancementSousActivite> getHistoriqueAvancement(Integer idSousActivite) {
                SousActivite sousActivite = sousActiviteRepository.findById(idSousActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Sous-activité introuvable",
                                                HttpStatus.NOT_FOUND));
                porteeService.exigerActivite(sousActivite.getActivite().getIdActivite());
                return avancementRepository.findHistorique(idSousActivite);
        }

        private Statut statutPourAvancement(int valeur) {
                String code = valeur == 0
                                ? StatutsActivite.NON_COMMENCEE
                                : valeur >= 100
                                                ? StatutsActivite.TERMINEE
                                                : StatutsActivite.EN_COURS;

                return statutRepository.findByCode(code)
                                .orElseThrow(() -> new ApiException(
                                                "Statut " + code + " introuvable dans le référentiel",
                                                HttpStatus.INTERNAL_SERVER_ERROR));
        }

        private String videSiBlanc(String v) {
                return (v == null || v.isBlank()) ? null : v.trim();
        }

        private void verifierAvancementAutorise(
                        Integer idActivite,
                        Integer idSousActivite,
                        Utilisateur utilisateur) {

                // 1. L'activité doit être affectée quelque part.
                long affectationsActivite = affectationActiviteRepository
                                .countActives(idActivite);
                long affectationsSousActivites = affectationSousActiviteRepository
                                .countActivesSurSousActivitesDeActivite(idActivite);

                if (affectationsActivite == 0 && affectationsSousActivites == 0) {
                        throw new ApiException(
                                        "L'activité n'est affectée à aucun utilisateur : "
                                                        + "son avancement ne peut pas être saisi.",
                                        HttpStatus.CONFLICT);
                }

                // 2. L'utilisateur courant doit être affecté (activité ou cette SA).
                long affectationsUtilisateurActivite = affectationActiviteRepository
                                .countActivesPourUtilisateur(idActivite, utilisateur.getIdUtilisateur());

                if (affectationsUtilisateurActivite > 0) {
                        return;
                }

                long affectationsUtilisateurSousActivite = affectationSousActiviteRepository
                                .countActivesPourUtilisateur(idSousActivite, utilisateur.getIdUtilisateur());

                if (affectationsUtilisateurSousActivite > 0) {
                        return;
                }

                throw new ApiException(
                                "Seuls les utilisateurs affectés à cette activité peuvent saisir "
                                                + "son avancement.",
                                HttpStatus.FORBIDDEN);
        }
}