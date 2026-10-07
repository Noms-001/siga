package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EtapeValidationResponse {
    private Integer id;
    private String designation;
    private String description;
    private Integer niveau;
    private Boolean obligatoire;
    private Boolean actif;
    private Boolean retour;
    private LocalDateTime dateDesactivation;
    private List<PosteDecideurResponse> decideurs;
}