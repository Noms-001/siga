package mg.bank.backend.dto.indicateur;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vue minimale d'un indicateur, utilisee pour la liste des indicateurs
 * disponibles a lier a une activite.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicateurSimpleDTO {
    private Integer id;
    private String code;
    private String designation;
    private String typeIndicateur;
    private String uniteMesure;
    private Boolean actif;
}