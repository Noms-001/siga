package mg.bank.backend.dto.backoffice;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtapeValidationRequest {

    /**
     * Null en création, renseigné en modification pour les étapes existantes.
     *
     * C'est ce champ qui distingue « mettre à jour l'étape X » de « créer une
     * nouvelle étape » : envoyer un id inconnu lève une 404, ne pas en envoyer
     * crée une ligne.
     */
    private Integer id;

    @NotBlank(message = "La désignation de l'étape est obligatoire")
    @Size(max = 255, message = "La désignation ne doit pas dépasser 255 caractères")
    private String designation;

    private String description;

    @NotNull(message = "Le caractère obligatoire est requis")
    private Boolean obligatoire;

    @NotNull(message = "Le retour est requis")
    private Boolean retour;

    /**
     * Identifiants des postes décideurs pour cette étape. Peut être vide,
     * mais une étape sans décideur ne pourra jamais être validée par
     * personne — c'est à l'auteur de la procédure d'y penser.
     */
    private List<Integer> idPostesDecideurs = new ArrayList<>();
}