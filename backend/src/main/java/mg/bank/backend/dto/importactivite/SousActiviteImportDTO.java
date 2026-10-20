package mg.bank.backend.dto.importactivite;

import java.time.LocalDate;
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
public class SousActiviteImportDTO {

    private String code;
    private String designation;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private LocalDate dateDebutReelle;

    private LocalDate dateFinReelle;

    private List<LivrableImportDTO> livrables;
}