package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionDetailResponse {
    private Integer id;
    private String designation;
    private String description;
    private String ressource;
    private String action;
    private Boolean actif;
    private LocalDateTime dateDesactivation;
    private List<PostePermissionResponse> postes;
}