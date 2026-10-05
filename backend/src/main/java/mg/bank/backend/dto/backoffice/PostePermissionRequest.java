package mg.bank.backend.dto.backoffice;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import mg.bank.backend.enums.PorteePermission;

@Getter
@Setter
public class PostePermissionRequest {

    @NotNull(message = "La permission est obligatoire")
    private Integer idPermission;

    @NotNull(message = "La portée est obligatoire")
    private PorteePermission portee;
}