package mg.bank.backend.dto.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 250, message = "Le nom ne doit pas dépasser 250 caractères")
    private String nom;

    private String description;

    @NotNull(message = "Le département est obligatoire")
    private Integer idDepartement;
}