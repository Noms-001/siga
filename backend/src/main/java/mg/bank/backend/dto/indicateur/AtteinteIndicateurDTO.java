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
public class AtteinteIndicateurDTO {
    private Integer idIndicateur;
    private String code;
    private String designation;
    private String typeIndicateur;
    private String uniteMesure;
    private BigDecimal valeurCible;
    private BigDecimal seuilMin;
    private BigDecimal seuilMax;

    /** Dernière valeur (période terminée si possible, sinon dernière saisie). */
    private BigDecimal valeurCourante;
    private LocalDateTime periodeDebut;
    private LocalDateTime periodeFin;

    /** Écart valeurCourante - valeurCible (null si l'un des deux est null). */
    private BigDecimal ecart;

    /**
     * KPI : ATTENTE si valeur >= cible, sinon NON_ATTEINT.
     * KRI : ATTENTE si valeur <= cible, sinon NON_ATTEINT.
     * INDETERMINE si une donnée manque.
     */
    private String statut;   // ATTEINT | NON_ATTEINT | INDETERMINE
}