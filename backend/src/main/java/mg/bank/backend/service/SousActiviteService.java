package mg.bank.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.AvancementRequest;
import mg.bank.backend.dto.AvancementResponse;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.AvancementSousActivite;
import mg.bank.backend.model.SousActivite;
import mg.bank.backend.model.Statut;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.AvancementSousActiviteRepository;
import mg.bank.backend.repository.SousActiviteRepository;
import mg.bank.backend.repository.StatutRepository;
import mg.bank.backend.repository.projection.AvancementSousActiviteRow;

@Service
@RequiredArgsConstructor
public class SousActiviteService {

        private final SousActiviteRepository sousActiviteRepository;
        private final AvancementSousActiviteRepository avancementRepository;
        private final StatutRepository statutRepository;
        private final ActiviteService activiteService;

        /**
         * Enregistre un nouvel avancement pour une sous-activité.
         *
         * RÈGLES MÉTIER PORTÉES ICI, PAS DANS LE FRONT
         *
         * Le frontend désactive le bouton dans les cas ci-dessous, mais la même
         * règle est vérifiée ici : un appel direct à l'API (curl, script) ne doit
         * pas pouvoir écrire un avancement incohérent. Le front est une aide à la
         * saisie, pas une garantie.
         *
         * 1. Valeur hors [0 ; 100] → 400.
         * 2. Valeur identique à la précédente → 400 (aucune écriture d'historique
         * inutile : ajouter une ligne pour dire la même chose brouillerait le
         * suivi).
         * 3. Diminution (nouvelle < ancienne) sans commentaire → 400. La
         * justification est la seule trace qui permette de comprendre plus
         * tard pourquoi un avancement a reculé.
         *
         * La règle "réouverture" (100 → <100) n'a pas de traitement spécial côté
         * backend : elle est couverte par la règle de diminution. Le front ajoute
         * un message plus explicite, le backend se contente de refuser sans
         * commentaire — la différence de formulation est d'usage, pas de droit.
         *
         * Aucun UPDATE, aucune suppression : l'historique est immuable. La valeur
         * courante d'une sous-activité est toujours la ligne la plus récente.
         */
        @Transactional
        public AvancementResponse enregistrerAvancement(
                        Integer idSousActivite,
                        AvancementRequest request) {

                SousActivite sousActivite = sousActiviteRepository.findById(idSousActivite)
                                .orElseThrow(() -> new ApiException(
                                                "Sous-activité introuvable",
                                                HttpStatus.NOT_FOUND));

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

                // Diminution sans justification : refus. Un premier relevé
                // (ancienneValeur == null) n'est pas une diminution, la règle ne
                // s'applique donc pas dans ce cas.
                if (ancienneValeur != null
                                && nouvelleValeur < ancienneValeur
                                && commentaire == null) {
                        throw new ApiException(
                                        "Une justification est obligatoire en cas de diminution "
                                                        + "de l'avancement.",
                                        HttpStatus.BAD_REQUEST);
                }

                Utilisateur utilisateur = activiteService.utilisateurCourant();
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

                return AvancementResponse.builder()
                                .idSousActivite(idSousActivite)
                                .ancienneValeur(ancienneValeur)
                                .avancement((double) nouvelleValeur)
                                .commentaire(commentaire)
                                .dateChangement(enregistrement.getDateChangement())
                                .statutCode(statut.getCode())
                                .statutLibelle(statut.getLibelle())
                                .avancementActivite(avancementActivite)
                                .build();
        }

        /**
         * Historique complet des avancements d'une sous-activité.
         *
         * La méthode existait dans le repository en SQL natif avec une projection
         * incomplète (3 champs) alors que la requête en renvoyait 9 : le mapping
         * était impossible. Le repository porte désormais une requête JPQL sur
         * l'entité, ce qui aligne le type de retour sur ce que le service déclare
         * et charge utilisateur + statut en une seule requête.
         *
         * Vérifie d'abord l'existence de la sous-activité : sans cela, une
         * sous-activité inconnue renverrait une liste vide, ce qui se confondrait
         * avec "aucun historique". Un 404 explicite lève l'ambiguïté.
         */
        @Transactional(readOnly = true)
        public List<AvancementSousActivite> getHistoriqueAvancement(Integer idSousActivite) {
                if (!sousActiviteRepository.existsById(idSousActivite)) {
                        throw new ApiException(
                                        "Sous-activité introuvable",
                                        HttpStatus.NOT_FOUND);
                }
                return avancementRepository.findHistorique(idSousActivite);
        }

        /**
         * Statut correspondant à un pourcentage.
         *
         * Les codes viennent de StatutsActivite — ils existent déjà en base,
         * on n'en crée pas de nouveau. Un statut absent lève une erreur
         * explicite plutôt qu'un NullPointerException silencieux : si la
         * référence est cassée, c'est un problème d'installation, pas un cas
         * à ignorer.
         */
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
}