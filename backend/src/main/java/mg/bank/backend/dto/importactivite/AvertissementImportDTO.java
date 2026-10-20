package mg.bank.backend.dto.importactivite;

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
public class AvertissementImportDTO {

    /** Reference, code, ou "activite #N". Null pour un avertissement d'entete. */
    private String activite;

    /** Chemin du champ concerne. */
    private String champ;

    /** Message comprehensible par l'utilisateur. */
    private String message;
}