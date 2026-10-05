package mg.bank.backend.dto.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 100, message = "Le code ne doit pas dépasser 100 caractères")
    private String code;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 150, message = "La désignation ne doit pas dépasser 150 caractères")
    private String designation;

    private String description;
}