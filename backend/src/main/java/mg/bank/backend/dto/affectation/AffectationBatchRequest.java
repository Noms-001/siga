package mg.bank.backend.dto.affectation;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class AffectationBatchRequest {

    @NotEmpty(message = "Au moins une affectation est requise")
    @Valid
    private List<Item> affectations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull
        private Integer utilisateurId;
        @NotNull
        private Integer roleId;
    }
}