package mg.bank.backend.dto.utilisateur;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.bank.backend.dto.activite.ResumeActivitesDTO;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.dto.planaction.ResumePlansActionDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilisateurAvancementDTO {
    private ReferenceDTO utilisateur;
    private int annee;
    private ResumeActivitesDTO activites;
    private ResumePlansActionDTO plansAction;
}