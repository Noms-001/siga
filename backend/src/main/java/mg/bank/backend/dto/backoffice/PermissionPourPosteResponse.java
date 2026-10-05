package mg.bank.backend.dto.backoffice;

import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.enums.PorteePermission;

/**
 * Vue d'une permission telle qu'elle apparaît dans le tableau du poste.
 *
 * On expose `idPermission` (et non pas `idPoste`) car le contexte de la page
 * est déjà « permissions de ce poste » : la clé qui identifie chaque ligne
 * est bien celle de la permission.
 */
@Getter
@Builder
public class PermissionPourPosteResponse {
    private Integer idPermission;
    private String designation;
    private String ressource;
    private String action;
    private Boolean actif;
    private PorteePermission portee;
}