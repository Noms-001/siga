package mg.bank.backend.dto.planaction;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanActionEcritureRequest {

    /** Facultatif : genere PA-<annee>-<numero> si absent. */
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String designation;

    @NotNull
    private LocalDate dateDebutPrevue;

    @NotNull
    private LocalDate dateFinPrevue;

    /** Au moins une origine obligatoire. */
    @NotEmpty
    private List<Integer> origineIds;

    /** Utilisateur a affecter comme RESPONSABLE. Obligatoire. */
    @NotNull
    private Integer responsableId;
}