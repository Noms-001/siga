package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Entree de l historique de statut d une activite.
 *
 * C est la SEULE source du statut courant d une activite : la table activite
 * n a pas de colonne de statut. La ligne dont le couple
 * (date_changement, id_historique_activite) est maximal est l etat courant,
 * et elle est la premiere de la liste, triee dans l ordre decroissant.
 *
 * Tri decroissant et non chronologique strict : deux changements peuvent
 * partager la meme date_changement, et c est alors la plus grande cle qui
 * tranche. Sans ce second critere, l ordre des lignes de meme date pourrait
 * changer d une requete a l autre.
 */
@Getter
@Builder
@AllArgsConstructor
public class HistoriqueActiviteDTO {

    private Integer id;

    private ReferenceDTO statut;

    private LocalDateTime dateChangement;

    private String commentaire;

    private UtilisateurResumeDTO utilisateur;

    /** Vrai pour la seule ligne portant le statut courant. */
    private boolean etatActuel;

}
