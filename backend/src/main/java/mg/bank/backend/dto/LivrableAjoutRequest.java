package mg.bank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Livrable tel qu ecrit depuis le detail d une sous-activite.
 *
 * Meme forme que ResultatIntermediaireEcritureRequest : la table
 * livrable_sous_activite ne porte que designation et description, donc il n y a
 * rien d autre a demander. Ni dates, ni quantite, ni unite, ni echeance : la
 * table ne les a pas, et les inventer ici reviendrait a accepter une saisie
 * qui irait dans le vide.
 *
 * Ce qui distingue cet appel de celui du formulaire de redaction, c est que
 * les FICHIERS ne voyagent pas dans ce corps. Ils sont joints en multipart, a
 * cote : ce DTO reste donc du JSON, validable par @Valid comme les autres, et
 * le transport ne force pas a assembler un document dans une chaine.
 *
 * La designation est seule obligatoire. La description cadre le livrable, elle
 * ne le definit pas.
 */
@Getter
@Setter
public class LivrableAjoutRequest {

    /**
     * VARCHAR(250) en base, d ou le meme plafond ici.
     *
     * Elle identifie le livrable dans la liste de sa sous-activite, et deux
     * libelles identiques sur la meme sous-activite sont refuses -- voir
     * LivrableRepository.existsBySousActiviteIdSousActivite
     * AndDesignationIgnoreCase.
     */
    @NotBlank(message = "La désignation du livrable est obligatoire")
    @Size(max = 250, message = "La désignation ne doit pas dépasser 250 caractères")
    private String designation;

    /**
     * Nullable : c est un texte de cadrage, pas une donnee d identification.
     *
     * Aucun @Size, et c est voulu : la colonne est un TEXT, sans plafond en
     * base, et LivrableEcritureRequest -- le meme champ, pour la meme donnee --
     * n en impose pas non plus. Inventer une limite ici reviendrait a refuser
     * une description que la base accepterait, et a le faire pour un seul des
     * deux chemins qui ecrivent ce champ.
     */
    private String description;

}
