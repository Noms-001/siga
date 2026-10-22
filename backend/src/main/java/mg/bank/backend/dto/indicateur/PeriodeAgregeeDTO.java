package mg.bank.backend.dto.indicateur;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class PeriodeAgregeeDTO {
    /** Ex. "T1 2026", "Janvier 2026", "S1 2026", "2026". */
    private String libelle;

    private LocalDateTime debut;
    private LocalDateTime fin;

    /** Résultat de l'agrégation sur la période. Null si aucune valeur. */
    private BigDecimal valeurAgregee;

    private int nombreSaisies;
}