package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PosteResponse {
    private Integer id;
    private String nom;
    private Integer effectifPrevu;
    private Integer effectifReel;
    private Boolean isMetier;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private LocalDateTime dateDesactivation;
}