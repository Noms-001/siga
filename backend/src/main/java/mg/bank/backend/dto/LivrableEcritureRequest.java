package mg.bank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Livrable tel qu'ecrit par le formulaire de redaction.
 *
 * Deux champs, parce que la table n'en porte que deux hors identifiant. Ni
 * quantite, ni unite, ni echeance, ni statut de reception : les exposer
 * reviendrait a creer une donnee sans colonne.
 *
 * Aucun identifiant n'est attendu, comme pour
 * ResultatIntermediaireEcritureRequest : la ligne appartient a sa
 * sous-activite, et c'est ce rattachement qui identifie la ligne, pas un
 * numero que le client choisirait.
 *
 * `description` n'a pas de @Size : la colonne est un TEXT et n'a donc pas de
 * plafond en base. En poser un ici reviendrait a interdire une valeur que la
 * base accepte, et a faire echouer l'enregistrement sur une regle qui n'existe
 * pas.
 */
@Getter
@Setter
public class LivrableEcritureRequest {

    @NotBlank(message = "La désignation du livrable est obligatoire")
    @Size(max = 250, message = "La désignation ne doit pas dépasser 250 caractères")
    private String designation;

    /**
     * Facultatif : la description cadre le livrable sans le definir, un
     * livrable sans description reste valide.
     */
    private String description;

}
