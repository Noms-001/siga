package mg.bank.backend.dto.planaction;

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
public class OrigineLienDTO {
    private Integer id;
    private String code;
    private String designation;
    private String typeOrigine;
    private Boolean estPrincipale;
}