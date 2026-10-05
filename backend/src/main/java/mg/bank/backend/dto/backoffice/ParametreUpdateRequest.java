package mg.bank.backend.dto.backoffice;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParametreUpdateRequest {

    @NotNull(message = "La valeur est obligatoire")
    private String valeur;
}