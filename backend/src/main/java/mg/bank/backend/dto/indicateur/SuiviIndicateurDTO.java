package mg.bank.backend.dto.indicateur;

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
public class SuiviIndicateurDTO {
    private Integer idActivite;
    private String code;
    private String designation;
    private List<AtteinteIndicateurDTO> indicateurs;
}