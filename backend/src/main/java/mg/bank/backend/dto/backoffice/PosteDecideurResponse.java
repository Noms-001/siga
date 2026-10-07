package mg.bank.backend.dto.backoffice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PosteDecideurResponse {
    private Integer id;
    private String nom;
    private Boolean isMetier;
}