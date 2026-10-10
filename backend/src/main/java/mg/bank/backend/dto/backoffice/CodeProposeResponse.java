package mg.bank.backend.dto.backoffice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CodeProposeResponse {
    private String code;
    private String typeOrigine;
    private Integer annee;
}