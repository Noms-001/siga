package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Fichier depose sur un livrable de sous-activite.
 *
 * cheminFichier est volontairement ABSENT du DTO. C est un emplacement de
 * stockage interne : l exposer ne rend aucun service tant qu aucun endpoint de
 * telechargement n existe, et il laisserait fuir l arborescence du serveur
 * pour une page qui affiche une liste de livrables. Seul nomFichier, qui
 * identifie le depot sans dire ou il se trouve, est renvoye.
 *
 * Le contenu binaire n est pas davantage expose : il ne transite jamais par
 * cette API.
 */
@Getter
@Builder
@AllArgsConstructor
public class FichierDetailDTO {

    private Integer id;

    /** Nom de stockage, different de nomOriginal apres un renommage. */
    private String nomFichier;

    /** Nom d origine, tel que fourni par l utilisateur. */
    private String nomOriginal;

    private String extension;

    private String typeMime;

    private Long taille;

    /** Rang du depot : deux versions d un meme nomOriginal coexistent. */
    private Integer version;

    private LocalDateTime dateDepot;

    private UtilisateurResumeDTO utilisateur;

}
