package mg.bank.backend.dto.importactivite;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SousActiviteImportRequest {

    private String version;

    /** Année de contrôle de l'import ; le code reprend l'année du parent. */
    private Integer annee;

    private List<ActiviteSousActivitesGroupDTO> activites;
}