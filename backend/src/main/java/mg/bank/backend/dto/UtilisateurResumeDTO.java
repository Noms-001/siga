package mg.bank.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Identite courte d un utilisateur, repete dans plusieurs collections du detail.
 *
 * L email et le telephone ne sont volontairement pas repris : le detail n a pas
 * besoin de coordonnees, et les exposer multiplierait les occurrences de donnees
 * personnelles pour un affichage qui se limite au nom.
 */
@Getter
@Builder
@AllArgsConstructor
public class UtilisateurResumeDTO {

    private Integer id;

    private String nom;

    private String prenom;

    public static UtilisateurResumeDTO videSi(Integer id, String nom, String prenom) {
        if (id == null) {
            return null;
        }

        return UtilisateurResumeDTO.builder()
                .id(id)
                .nom(nom)
                .prenom(prenom)
                .build();
    }

}
