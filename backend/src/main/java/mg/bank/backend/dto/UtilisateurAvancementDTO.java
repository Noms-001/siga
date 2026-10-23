package mg.bank.backend.dto;

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
public class UtilisateurAvancementDTO {
    private ReferenceDTO utilisateur;
    private int annee;
    private ResumeActivitesDTO activites;
    private ResumePlansActionDTO plansAction;
}