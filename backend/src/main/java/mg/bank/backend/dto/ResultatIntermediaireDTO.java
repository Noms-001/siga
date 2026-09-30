package mg.bank.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Resultat intermediaire rattache a une activite.
 *
 * La table ne porte que trois colonnes : un identifiant, une designation et
 * l activite. Aucun commentaire, aucune date, aucune valeur. Le DTO s en tient
 * a ce qui existe plutot que d inventer des champs vides.
 */
@Getter
@Builder
@AllArgsConstructor
public class ResultatIntermediaireDTO {

    private Integer id;

    private String designation;

}
