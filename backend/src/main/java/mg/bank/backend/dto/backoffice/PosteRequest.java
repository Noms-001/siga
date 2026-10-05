package mg.bank.backend.dto.backoffice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosteRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
    private String nom;

    @NotNull(message = "L'effectif prévu est obligatoire")
    @Min(value = 0, message = "L'effectif prévu doit être supérieur ou égal à 0")
    private Integer effectifPrevu;

    @Min(value = 0, message = "L'effectif réel doit être supérieur ou égal à 0")
    private Integer effectifReel;

    /**
     * Nullable : par défaut, un poste est métier (cohérent avec la colonne
     * `is_metier` qui est NOT NULL DEFAULT TRUE).
     */
    private Boolean isMetier;
}