package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrigineResponse {
    private Integer id;
    private String code;
    private String designation;
    private String typeOrigine;
    private String description;
    private LocalDateTime dateCreation;
    private Integer idUtilisateur;
    private String nomUtilisateur;
    private String prenomUtilisateur;
}