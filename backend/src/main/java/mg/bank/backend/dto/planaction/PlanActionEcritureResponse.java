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
public class PlanActionEcritureResponse {
    private Integer id;
    private String code;
    private String designation;
    private String statutInitial;
}