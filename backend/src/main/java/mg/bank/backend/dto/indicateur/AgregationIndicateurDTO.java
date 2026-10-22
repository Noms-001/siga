package mg.bank.backend.dto.indicateur;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
public class AgregationIndicateurDTO {
    private Integer idIndicateur;
    private String code;
    private String designation;
    private String uniteMesure;
    private String typeIndicateur;

    /** Frequence lue sur l'indicateur (MENSUEL, TRIMESTRIEL, ...). */
    private String frequenceAggregation;

    /** Methode demandee par l'appelant. */
    private String methode;

    private LocalDateTime debut;
    private LocalDateTime fin;

    /** Agrégation globale sur toute la plage (même méthode). */
    private BigDecimal valeurGlobale;
    private int nombreSaisiesTotal;

    /** Agrégation par sous-période. */
    private List<PeriodeAgregeeDTO> periodes;
}