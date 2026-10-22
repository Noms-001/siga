package mg.bank.backend.dto.affectation;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.bank.backend.dto.ReferenceDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationActiviteDTO {
    private Integer id;
    private LocalDateTime dateAffectation;
    private LocalDateTime dateDesaffectation;
    private ReferenceDTO role;
    private ReferenceDTO utilisateur;
    private boolean active;
}