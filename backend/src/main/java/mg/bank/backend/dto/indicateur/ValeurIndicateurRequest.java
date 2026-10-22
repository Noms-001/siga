package mg.bank.backend.dto.indicateur;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class ValeurIndicateurRequest {

    @NotNull(message = "La valeur est obligatoire")
    private BigDecimal valeur;

    @NotNull(message = "La période de début est obligatoire")
    private LocalDateTime periodeDebut;

    @NotNull(message = "La période de fin est obligatoire")
    private LocalDateTime periodeFin;

    private String commentaire;
}