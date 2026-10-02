package mg.bank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Resultat intermediaire tel qu ecrit par le formulaire de redaction.
 *
 * Un seul champ, la designation, parce que la table n en porte pas d'autre.
 *
 * Aucun identifiant n est attendu, contrairement a SousActiviteEcritureRequest.
 * Une ligne de resultat n est attachee qu a son activite et n a aucune
 * dependance : la modifier consiste a remplacer l ensemble, ce qui n a pas a
 * etre reconcilie ligne a ligne. Renvoyer un identifiant ici ferait croire a
 * une synchronisation que le backend n a pas lieu de faire.
 */
@Getter
@Setter
public class ResultatIntermediaireEcritureRequest {

    @NotBlank(message = "La désignation du résultat intermédiaire est obligatoire")
    @Size(max = 255, message = "La désignation ne doit pas dépasser 255 caractères")
    private String designation;

}
