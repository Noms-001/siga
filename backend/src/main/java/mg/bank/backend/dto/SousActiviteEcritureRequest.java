package mg.bank.backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Sous-activite telle qu ecrite par le formulaire de redaction.
 *
 * Champs volontairement absents, parce que la table ne les porte pas :
 * description, responsable, statut, avancement. Le responsable passe par
 * affectation_sous_activite et l avancement par avancement_sous_activite :
 * les exposer ici reviendrait a creer une donnee sans colonne.
 *
 * idSousActivite : absent ou null = creation, renseignee = modification de
 * cette ligne. C est ce qui permet de synchroniser un lot sans supprimer
 * puis recreer toutes les sous-activites.
 */
@Getter
@Setter
public class SousActiviteEcritureRequest {

    // Positive, et surtout PAS Size : cet identifiant est un entier. Un @Size
    //posé ici était un copier-coller de la contrainte du code, et il rendait la
    // validation INVALIDE plutot que fausse -- Hibernate Validator ne sait pas
    // borner un Integer, il leve UnexpectedTypeException, et toute creation
    // d'activite avec sous-activites repondait 500. Le champ peut legitimement
    // rester vide, d'ou l'absence de @NotNull.
    @Positive(message = "L'identifiant de la sous-activité doit être positif")
    private Integer idSousActivite;

    /**
     * Facultatif : si absent, le backend genere un code unique a partir de
     * l identifiant de l activite, dans le format deja utilise par
     * data.sql (SA-<idActivite>-<n>).
     */
    @Size(max = 20, message = "Le code ne doit pas dépasser 20 caractères")
    private String code;

    @NotBlank(message = "La désignation de la sous-activité est obligatoire")
    @Size(max = 255, message = "La désignation ne doit pas dépasser 255 caractères")
    private String designation;

    @NotNull(message = "La date de début prévue est obligatoire")
    private LocalDate dateDebutPrevue;

    /**
     * NOT NULL dans le schema, contrairement a activite.date_fin_prevue :
     * une sous-activite a une fin.
     */
    @NotNull(message = "La date de fin prévue est obligatoire")
    private LocalDate dateFinPrevue;

    /**
     * Livrables attendus de cette sous-activite.
     *
     * Vide ou absent = sous-activite sans livrable, ce qui est autorise.
     *
     * Les livrables sont imbriques dans la sous-activite et non dans une
     * collection a part : la table ne depend que de sous_activite, et
     *LivrableDetailDTO fait de meme pour les fichiers.
     *
     * Les fichiers deposes ne sont pas renvoyes ni acceptes ici : le depot
     * releve du suivi, et un livrable qui en porte ne peut pas etre
     * supprime.
     */
    @Valid
    private List<LivrableEcritureRequest> livrables = new ArrayList<>();

}
