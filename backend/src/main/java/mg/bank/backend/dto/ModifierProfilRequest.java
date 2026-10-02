package mg.bank.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Modification des informations personnelles par l'utilisateur lui-même.
 *
 * Les champs administrés (departement, service, postes) ne sont pas
 * modifiables depuis ce point d'entrée : ils relèvent du backoffice.
 */
@Getter
@Setter
public class ModifierProfilRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 250, message = "Le nom ne doit pas dépasser 250 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 250, message = "Le prénom ne doit pas dépasser 250 caractères")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email est invalide")
    @Size(max = 100, message = "L'email ne doit pas dépasser 100 caractères")
    private String email;

    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    private String telephone;
}
