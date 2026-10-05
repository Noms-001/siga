package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionResponse {
    private Integer id;
    private String designation;
    private String description;
    private String ressource;
    private String action;
    private Boolean actif;
    private LocalDateTime dateDesactivation;
}