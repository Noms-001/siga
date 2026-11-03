package mg.bank.backend.dto.notification;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.dto.common.UtilisateurResumeDTO;

@Getter
@Builder
@AllArgsConstructor
public class NotificationDetailDTO {

    private Integer id;
    private String titre;
    private String message;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLecture;
    private ReferenceDTO priorite;
    private UtilisateurResumeDTO utilisateur;

    /** ACTIVITE / SOUS_ACTIVITE / PLAN_ACTION / INDICATEUR. Null si absent. */
    private String resource;

    private Integer idReference;

    /** Ex : "TSA-A1-03 — En retard". Null si entite absente. */
    private String libelleReference;

    /** Chemin front pour naviguer vers l'entite. Null si non mappe. */
    private String urlFront;

    /** Faux si l'entite de reference n'existe plus en base. */
    private boolean entiteDisponible;

    /** Libelle -> valeur pre-formatee. Jamais null (vide si aucune). */
    private Map<String, String> contexte;
}