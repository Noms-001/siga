package mg.bank.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvancementRequest {

    @NotNull(message = "La valeur d'avancement est obligatoire")
    @Min(value = 0, message = "L'avancement ne peut pas être négatif")
    @Max(value = 100, message = "L'avancement ne peut pas dépasser 100")
    private Integer valeurPourcentage;

    @Size(max = 2000, message = "Le commentaire ne doit pas dépasser 2000 caractères")
    private String commentaire;
}