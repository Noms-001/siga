package mg.bank.backend.dto.backoffice;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionFiltresResponse {
    private List<String> ressources;
    private List<String> actions;
}