package mg.bank.backend.dto.planaction;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class AvancementSaisieRequest {

    @NotNull(message = "La valeur d'avancement est obligatoire")
    @DecimalMin(value = "0.00", message = "L'avancement doit être supérieur ou égal à 0")
    @DecimalMax(value = "100.00", message = "L'avancement doit être inférieur ou égal à 100")
    private BigDecimal valeurPourcentage;

    private String commentaire;
}