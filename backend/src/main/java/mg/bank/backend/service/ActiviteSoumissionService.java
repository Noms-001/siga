package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ActiviteSoumissionDTO;
import mg.bank.backend.dto.PerimetreUtilisateur;
import mg.bank.backend.enums.StatutsActivite;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Activite;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.ActiviteRepository;
import mg.bank.backend.repository.projection.StatutCourantRow;

/**
 * Ecriture : passage d une activite du statut BROUILLON au statut
 * EN_ATTENTE_VALIDATION.
 *
 * CE QUI FAIT UNE TRANSITION
 * Changer le statut d une activite ne met rien a jour dans activite, qui ne
 * porte pas de colonne statut. La transition EST une ligne ajoutee dans
 * historique_activite, avec le statut cible et l auteur du changement. Le
 * statut courant est donc toujours le max(date_changement,
 * id_historique_activite) de cette activite, et c est ce que
 * findStatutCourant relit.
 *
 * CE QUI EST REFUSE, ET PAR QUOI
 * Trois echecs distincts, parce qu ils ne se corrigent pas de la meme facon :
 *
 * - activite absente du perimetre ou inexistante : ce n est pas une erreur de
 *   l appelant mais une donnee qui n est pas sienne, et le depot n a pas
 *   lieu d etre tente. Verification faite par le perimetre, jamais par un
 *   parametre HTTP.
 * - activite deja soumise, ou dans un autre etat : le front peut le
 *  rencontrer si deux onglets sont ouverts sur la meme activite. Conflit,
 *   pas erreur de saisie.
 * - identifiant mal forme : erreur du client.
 *
 * L ecriture est transactionnelle et le verrou de ligne est pris AVANT la
 * relecture du statut. Sans cela, deux soumissions simultanees du meme
 * brouillon pourraient toutes deux lire BROUILLON et deposer deux lignes
 * EN_ATTENTE_VALIDATION : le verrou fait echouer la seconde.
 *
 * AUCUNE VALIDATION METIER ICI
 * Ce service ne verifie ni le contenu de l activite, ni la presence de
 * sous-activites, ni les dates. Son unique regle est "BROUILLON devient
 * EN_ATTENTE_VALIDATION". Les regles de completude eventuelles apparaitront
 * ici, et nowhere ailleurs.
 */
@Service
@RequiredArgsConstructor
public class ActiviteSoumissionService {

    private final ActiviteRepository activiteRepository;

    private final ActiviteService activiteService;

    /**
     * Soumettre une activite a la validation.
     *
     * Idempotence par le conflit : une seconde soumission du meme brouillon
     * ne renvoie pas un succes, elle echoue en 409. Rendre l appel idempotent
     * masquerait a l appelant que l activite a deja quitte les brouillons.
     */
    @Transactional
    public ActiviteSoumissionDTO soumettre(Integer idActivite) {

        if (idActivite == null || idActivite < 1) {
            throw new ApiException(
                    "L identifiant de l activite doit etre un entier positif",
                    HttpStatus.BAD_REQUEST);
        }

        PerimetreUtilisateur perimetre = activiteService.getPerimetre();
        Utilisateur auteur = activiteService.utilisateurCourant();

        Activite verrouillee = activiteRepository.verrouillerActivite(idActivite)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        StatutCourantRow courant = statutCourant(idActivite, perimetre)
                .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));

        if (!StatutsActivite.BROUILLON.equals(courant.getStatutCode())) {
            throw new ApiException(
                    "Seule une activite en brouillon peut etre soumise a la validation",
                    HttpStatus.CONFLICT);
        }

        activiteRepository.ajouterChangementStatut(
                idActivite,
                auteur.getIdUtilisateur(),
                StatutsActivite.EN_ATTENTE_VALIDATION,
                "Soumission a la validation");

        /*
            La soumission ouvre aussi une demande dans le circuit de validation,
            sur la premiere etape active.

            SANS ELLE, LES DEUX MODELES DIVERGENT : l activite serait en attente
            d etat mais introuvable par la page des validateurs, et une decision
            prise dessus echouerait faute de demande. C est la ligne qui rend
            coherents l historique d etat et le circuit d examen.

            Elle est facultative, et son absence n annule pas la soumission : sans
            etape active configuree, il n y a personne a qui demander, et le
            statut reste le bon. Le controller l annonce dans sa reponse.
        */
        Integer premiereEtape = activiteRepository.findPremiereEtapeActive();

        if (premiereEtape != null) {
            activiteRepository.ouvrirDemandeValidation(
                    idActivite,
                    premiereEtape,
                    auteur.getIdUtilisateur(),
                    auteur.getIdUtilisateur(),
                    "Demande de validation");
        }

        return ActiviteSoumissionDTO.builder()
                .idActivite(verrouillee.getIdActivite())
                .code(verrouillee.getCode())
                .statut(StatutsActivite.EN_ATTENTE_VALIDATION)
                .dateSoumission(LocalDateTime.now())
                .build();
    }

    /**
     * Statut courant, restreint au perimetre.
     *
     * Absent = activite inexistante OU hors perimetre : les deux cas sont
     * traites identiquement par l appelant, qui ne doit pas distinguer une
     * donnee absente d une donnee qu il ne voit pas.
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
