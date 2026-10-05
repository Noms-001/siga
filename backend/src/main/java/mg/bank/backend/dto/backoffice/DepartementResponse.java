package mg.bank.backend.dto.backoffice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartementResponse {
    private Integer id;
    private String code;
    private String nom;
    private String description;
}