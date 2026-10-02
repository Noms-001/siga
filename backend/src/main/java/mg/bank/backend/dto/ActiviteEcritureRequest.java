package mg.bank.backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Corps commun a la creation et a la modification d une activite.
 *
 * Un seul DTO pour les deux operations : le formulaire de modification
 * pre-remplit exactement les memes champs, et un second DTO qui en
 * recopierait les regles deriverait des deux cotes.
 *
 * AUCUN STATUT DANS CE CORPS
 * activite ne porte pas de colonne statut, et le statut d une activite
 * neuve est impose par le backend a BROUILLON. Un statut recu du client
 * rendrait la creation arbitraire et permettrait d injecter une activite
 * directement dans le circuit de suivi.
 */
@Getter
@Setter
public class ActiviteEcritureRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 20, message = "Le code ne doit pas dépasser 20 caractères")
    private String code;

    /**
     * Plus @NotBlank, et le deplacement est volontaire.
     *
     * L annotation imposait une reference a TOUTE activite, ce qui exclutait
     * les NPTA : celles-ci ne sont rattachees a aucun objectif, donc leur
     * reference n a rien a designer. La colonne est nullable en base, et une
     * contrainte declarative ne peut pas dire "obligatoire sauf si le corps
     * declare pta = false".
     *
     * La regle est donc verifiee cote service, au meme endroit que la
     * coherence de idObjectifSpecifique, qui se joue deja dans les deux sens :
     * PTA = renseigne, NON PTA = absent. Voir
     * ActiviteEcritureService.resoudreReference.
     */
    @Size(max = 20, message = "La référence ne doit pas dépasser 20 caractères")
    private String reference;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 255, message = "La désignation ne doit pas dépasser 255 caractères")
    private String designation;

    @NotNull(message = "La date de début prévue est obligatoire")
    private LocalDate dateDebutPrevue;

    /**
     * Nullable : une activite peut etre ouverte sans echeance.
     */
    private LocalDate dateFinPrevue;

    /**
     * Le formulaire declare le type d activite plutot que de le laisser
     * deduire.
     *
     * Sans ce drapeau, la coherence PTA / NON PTA ne serait pas verifiable :
     * idObjectifSpecifique suffirait a qualifier l activite, et une
     * contradiction du client ne pourrait jamais etre detectee. Le garder
     * rend la regle testable et refuse un corps qui se contredit
     * lui-meme.
     */
    @NotNull(message = "Le type d'activité (PTA ou non PTA) est obligatoire")
    private Boolean pta;

    /**
     * PTA = renseigne, NON PTA = absent. La coherence des deux est
     * verifiee cote service, jamais laissee a l affichage du front.
     */
    private Integer idObjectifSpecifique;

    private Integer idTypeActivite;

    private Integer idSite;

    @NotNull(message = "La priorité est obligatoire")
    private Integer idPriorite;

    /**
     * Facultatif dans le corps, et c est volontaire.
     *
     * activite.id_service est NOT NULL, mais un utilisateur rattache a un
     * service n a pas de liste de services a choisir (options.services est
     * vide pour lui) et n a aucun moyen de connaitre l identifiant de son
     * propre service : ProfileResponse n expose que son nom. Exiger
     * l identifiant dans le corps obligerait soit a envoyer null, soit a
     * elargir ProfileResponse.
     *
     * Le service tranche donc cote backend : un utilisateur rattache a un
     * service voit son service impose, quel que soit ce qu il envoie ; un
     * utilisateur de niveau departement doit, lui, choisir parmi les
     * services de son departement.
     */
    private Integer idService;

    /**
     * Vide ou absent = activite sans sous-activite, ce qui est autorise.
     * Pas de valeur par defaut vide allouee : null et liste vide doivent
     * rester indistinguables cote metier.
     */
    @Valid
    private List<SousActiviteEcritureRequest> sousActivites = new ArrayList<>();

    /**
     * Resultats intermediaires de l'activite.
     *
     * Vide ou absent = activite sans resultat intermediaire, ce qui est
     * autorise. Les lignes sont remplacees a l'enregistrement, et non
     * synchronisees identifiant par identifiant : aucune table ne depend de
     * resultat_intermediaire, donc une ligne supprimee n emporte rien.
     *
     * @Valid est indispensable, sans quoi une designation vide passerait
     * jusqu a la base et y echouerait sur une contrainte NOT NULL, en 500
     * et non en 400.
     */
    @Valid
    private List<ResultatIntermediaireEcritureRequest> resultatsIntermediaires
            = new ArrayList<>();

}
