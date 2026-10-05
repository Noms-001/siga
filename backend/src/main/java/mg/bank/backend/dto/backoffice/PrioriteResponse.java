package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrioriteResponse {
    private Integer id;
    private String code;
    private String libelle;
    private String description;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDesactivation;
}