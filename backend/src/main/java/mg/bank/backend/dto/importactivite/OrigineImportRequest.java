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
public class OrigineImportRequest {

    private String version;

    private List<ActiviteOriginesGroupDTO> activites;
}