package mg.bank.backend.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Activite rechargee dans le formulaire, en mode modification.
 *
 * Alimenté par l endpoint de creation/modification lui-meme, et non par le
 * detail : le detail renvoie sous-activites, avancement, livrables,
 * fichiers, indicateurs et historique, soit beaucoup plus que
 * ce que le formulaire edite. Un sous-ensemble dedie evite au front de
 * deviner quels champs sont editables.
 */
@Getter
@Builder
public class ActiviteFormulaireDTO {

    private Integer idActivite;

    private String code;

    private String reference;

    private String designation;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private Integer idObjectifSpecifique;

    /**
     * Objectif retenu, en clair.
     *
     * Un identifiant seul ne suffit pas a pre-remplir un champ : le
     * formulaire doit afficher un libelle comprehensible, pas un nombre. Il
     * est renvoye ici plutot que laisse a l autocomplete du front, car une
     * saisie par identifiant n est pas une recherche, et rejouer une
     * recherche pour retrouver un libelle deja connu serait fragile.
     *
     * Les trois valeurs sont nulles ensemble, quand aucun objectif n est
     * rattache.
     */
    private String objectifCode;

    private String objectifDesignation;

    private Integer objectifAnnee;

    private Integer idTypeActivite;

    private Integer idSite;

    private Integer idPriorite;

    private Integer idService;

    /**
     * Statut courant, pour que le front n affiche le bouton Modifier que
     * sur un brouillon. Ce n est qu un affichage : le controle reel est
     * refait par le backend, un bouton masque n etant pas une
     * autorisation.
     */
    private String statut;

    private List<SousActiviteFormulaireDTO> sousActivites;

    /**
     * Resultats intermediaires deja enregistres, dans l'ordre de saisie.
     *
     * Renvoyes pour que le formulaire de modification les propose au lieu
     * d en repartir d une liste vide : sinon toute relecture d un brouillon
     * effacerait ses resultats intermediaires a l'enregistrement suivant.
     */
    private List<ResultatIntermediaireFormulaireDTO> resultatsIntermediaires;

}
