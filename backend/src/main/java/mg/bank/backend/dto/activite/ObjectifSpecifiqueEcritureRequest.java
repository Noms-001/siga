package mg.bank.backend.dto.activite;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObjectifSpecifiqueEcritureRequest {

    @NotBlank(message = "Le code de l'objectif est obligatoire")
    @Size(max = 20, message = "Le code ne doit pas dépasser 20 caractères")
    private String code;

    @NotBlank(message = "La désignation de l'objectif est obligatoire")
    private String designation;

    @NotNull(message = "L'année de l'objectif est obligatoire")
    @Min(value = 2000, message = "L'année doit être comprise entre 2000 et 2100")
    @Max(value = 2100, message = "L'année doit être comprise entre 2000 et 2100")
    private Integer annee;

}
