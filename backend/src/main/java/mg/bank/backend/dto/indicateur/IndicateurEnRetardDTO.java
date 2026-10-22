package mg.bank.backend.dto.indicateur;

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
public class IndicateurEnRetardDTO {
    private Integer idIndicateur;
    private String code;
    private String designation;
    private String typeIndicateur;
    private String frequenceVerification;

    /** Dernière période de saisie terminée. Null si aucune saisie. */
    private LocalDateTime dernierePeriodeFin;

    /** dernierePeriodeFin + fréquence. Null si aucune saisie. */
    private LocalDateTime prochaineEcheance;

    /** Nombre de jours entre prochaineEcheance et aujourd'hui. Null si aucune saisie. */
    private Long joursDeRetard;

    /** EN_RETARD ou JAMAIS_SAISI. */
    private String statut;
}