package mg.bank.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Objectif specifique a creer depuis le formulaire d'activite.
 *
 * Les trois champs sont ceux de la table, et ils sont tous necessaires :
 *
 * - `code` est UNIQUE, donc il identifie l'objectif. C'est aussi le prefixe
 *   de la reference d'activite proposee ensuite (`BCT/1_3`), ce qui rend la
 *   duplication d'un code silencieux et trompeur.
 * - `designation` est NOT NULL : un objectif sans intitule n'a pas d'usage
 *   dans une liste de choix.
 * - `annee` est NOT NULL et contrainte entre 1900 et 2100, et n'est pas
 *   derivable du code : le code du seed est `BCT/1`, sans annee. Elle ne
 *   peut donc pas etre inventeee cote backend, ni deduite de la saisie. D'ou
 *   un champ, plutot qu'une valeur par defaut qui rangerait l'utilisateur
 *   dans l'annee du jour sans qu'il l'ait demandee.
 *
 * Aucun autre champ n'est accepte : la table n'en porte pas d'autre, et
 * `date_creation` a une valeur par defaut en base.
 */
@Getter
@Setter
public class ObjectifSpecifiqueEcritureRequest {

    @NotBlank(message = "Le code de l'objectif est obligatoire")
    @Size(max = 20, message = "Le code ne doit pas dépasser 20 caractères")
    private String code;

    @NotBlank(message = "La désignation de l'objectif est obligatoire")
    @Size(max = 250, message = "La désignation ne doit pas dépasser 250 caractères")
    private String designation;

    /**
     * Reprise de chk_objectif_specifique_annee. Annoncee ici pour que
     * l'utilisateur recoive le message precis plutot qu'un 400 generique,
     * et pour que la regle soit lisible a cote de la saisie qu'elle borne.
     */
    @NotNull(message = "L'année de l'objectif est obligatoire")
    @Min(value = 1900, message = "L'année doit être comprise entre 1900 et 2100")
    @Max(value = 2100, message = "L'année doit être comprise entre 1900 et 2100")
    private Integer annee;

}
