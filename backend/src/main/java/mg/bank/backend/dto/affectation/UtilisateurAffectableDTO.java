package mg.bank.backend.dto.affectation;

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
public class UtilisateurAffectableDTO {
    private Integer id;
    private String nom;
    private String prenom;
}