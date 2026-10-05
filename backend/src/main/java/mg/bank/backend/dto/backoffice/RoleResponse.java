package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleResponse {
    private Integer id;
    private String code;
    private String designation;
    private String description;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private LocalDateTime dateDesactivation;
}