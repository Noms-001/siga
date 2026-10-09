package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue détaillée d'un signalement.
 *
 * Se distingue de OrigineResponse (utilisé par la liste) par la
 * présence de `plansAction`. La liste ne charge PAS cette collection :
 * la charger pour chaque ligne produirait un N+1 certain. C'est le
 * détail d'UN signalement qui vaut la requête supplémentaire.
 */
@Getter
@Builder
public class SignalementDetailResponse {
    private Integer id;
    private String code;
    private String designation;
    private String typeOrigine;
    private String description;
    private LocalDateTime dateCreation;
    private Integer idUtilisateur;
    private String nomUtilisateur;
    private String prenomUtilisateur;
    private List<PlanActionResumeDTO> plansAction;
}