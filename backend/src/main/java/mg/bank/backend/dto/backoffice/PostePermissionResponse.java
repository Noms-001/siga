package mg.bank.backend.dto.backoffice;

import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.enums.PorteePermission;

@Getter
@Builder
public class PostePermissionResponse {
    private Integer idPoste;
    /** Nom du poste — mappé depuis Poste.libelle (colonne poste.nom). */
    private String nom;
    private PorteePermission portee;
}