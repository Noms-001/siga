package mg.bank.backend.dto.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrigineRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 50, message = "Le code ne doit pas dépasser 50 caractères")
    private String code;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 255, message = "La désignation ne doit pas dépasser 255 caractères")
    private String designation;

    @NotBlank(message = "Le type est obligatoire")
    @Size(max = 50, message = "Le type ne doit pas dépasser 50 caractères")
    private String typeOrigine;

    private String description;
}