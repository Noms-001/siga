package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SiteResponse {
    private Integer id;
    private String nom;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDesactivation;
}