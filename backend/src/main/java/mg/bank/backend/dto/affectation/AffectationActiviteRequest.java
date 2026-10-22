package mg.bank.backend.dto.affectation;

import jakarta.validation.constraints.NotNull;
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
public class AffectationActiviteRequest {

    @NotNull(message = "L'utilisateur à affecter est obligatoire")
    private Integer utilisateurId;

    @NotNull(message = "Le rôle est obligatoire")
    private Integer roleId;
}