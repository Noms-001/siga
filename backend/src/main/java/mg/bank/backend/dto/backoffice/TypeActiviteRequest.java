package mg.bank.backend.dto.backoffice;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TypeActiviteRequest {

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 100, message = "La désignation ne doit pas dépasser 100 caractères")
    private String designation;

    private String description;

    /**
     * Identifiants des services à associer. Liste vide ou null → aucune
     * association (le type d'activité reste valide en soi).
     */
    private Set<Integer> idServices = new HashSet<>();
}