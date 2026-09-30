package mg.bank.backend.repository.projection;

import java.time.LocalDateTime;

/**
 * Projection d une entree de l historique de statut d une activite.
 *
 * La requete qui alimente cette projection trie par date decroissante, et
 * c est de cet ordre que depend la designation de la ligne courante : la
 * premiere ligne renvoyee est l etat actuel. Le drapeau n est pas calcule en
 * base, il est pose par le service en comparant chaque ligne a la premiere.
 * Cela evite de dupliquer dans le SQL un critere de selection qui doit rester
 * identique a celui du tri.
 */
public interface HistoriqueActiviteRow {

    Integer getId();

    LocalDateTime getDateChangement();

    String getCommentaire();

    Integer getStatutId();

    String getStatutCode();

    String getStatutLibelle();

    Integer getUtilisateurId();

    String getUtilisateurNom();

    String getUtilisateurPrenom();

}
