package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParametreResponse {
    private Integer id;
    private String code;
    private String designation;
    private String valeur;
    private String typeValeur;
    private String description;
    private String categorie;
    private Boolean modifiable;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private LocalDateTime dateDesactivation;
    private Integer idUtilisateurModification;
    private String nomUtilisateurModification;
}