package mg.bank.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilisateurRequest {

    @NotNull(message = "Le département est obligatoire")
    private Integer idDepartement;

    private Integer idService;

    @NotNull(message = "Le poste est obligatoire")
    private Integer idPoste;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email est invalide")
    private String email;

    private String telephone;
}