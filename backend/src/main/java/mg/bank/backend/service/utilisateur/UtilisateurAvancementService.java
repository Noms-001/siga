package mg.bank.backend.service.utilisateur;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.activite.ActiviteAvancementDTO;
import mg.bank.backend.dto.activite.ResumeActivitesDTO;
import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.dto.planaction.PlanActionAvancementDTO;
import mg.bank.backend.dto.planaction.ResumePlansActionDTO;
import mg.bank.backend.dto.utilisateur.UtilisateurAvancementDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.planaction.PlanActionRepository;
import mg.bank.backend.repository.projection.activite.UtilisateurActiviteAnneeRow;
import mg.bank.backend.repository.projection.planaction.UtilisateurPlanActionAnneeRow;
import mg.bank.backend.repository.utilisateur.UtilisateurRepository;

/**
 * Avancement d'un utilisateur sur une annee : liste des activites et
 * plans d'action auxquels il est affecte, avec leur avancement courant.
 *
 * <p>Regles :</p>
 * <ul>
 *   <li>tous les roles sont inclus (RESPONSABLE, SUPPLEANT, PARTICIPANT) ;</li>
 *   <li>seules les affectations actives sont prises en compte
 *       (date_desaffectation IS NULL) ;</li>
 *   <li>une entite touche l'annee si dateDebutPrevue <= 31/12/N et
 *       (dateFinPrevue IS NULL ou dateFinPrevue >= 01/01/N) ;</li>
 *   <li>l'avancement moyen est calcule sur TOUTES les entites (tous
 *       roles confondus) ;</li>
 *   <li>joursDeRetard : 0 si terminee a temps, date_fin_reelle -
 *       date_fin_prevue si terminee en retard, CURRENT_DATE -
 *       date_fin_prevue si non terminee apres l'echeance.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UtilisateurAvancementService {

    /** Statuts terminaux qui ne declenchent pas de retard. */
    private static final Set<String> STATUTS_TERMINAUX =
            Set.of("TERMINEE", "ANNULEE", "REJETEE");

    private final UtilisateurRepository utilisateurRepository;
    private final ActiviteRepository activiteRepository;
    private final PlanActionRepository planActionRepository;

    @Transactional(readOnly = true)
    public UtilisateurAvancementDTO avancementAnnee(Integer idUtilisateur, Integer annee) {

        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new ApiException(
                        "Utilisateur introuvable.", HttpStatus.NOT_FOUND));

        int anneeCible = (annee != null) ? annee : LocalDate.now().getYear();
        LocalDate debutAnnee = LocalDate.of(anneeCible, 1, 1);
        LocalDate finAnnee = LocalDate.of(anneeCible, 12, 31);

        List<UtilisateurActiviteAnneeRow> activites = activiteRepository
                .findActivitesAnneePourUtilisateur(idUtilisateur, debutAnnee, finAnnee);

        List<UtilisateurPlanActionAnneeRow> plans = planActionRepository
                .findPlansAnneePourUtilisateur(idUtilisateur, debutAnnee, finAnnee);

        return UtilisateurAvancementDTO.builder()
                .utilisateur(ReferenceDTO.builder()
                        .id(utilisateur.getIdUtilisateur())
                        .libelle(utilisateur.getPrenom() + " " + utilisateur.getNom())
                        .build())
                .annee(anneeCible)
                .activites(construireResumeActivites(activites))
                .plansAction(construireResumePlans(plans))
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers — activités
    // ------------------------------------------------------------------

    private ResumeActivitesDTO construireResumeActivites(
            List<UtilisateurActiviteAnneeRow> rows) {

        int nonCommencees = 0;
        int enCours = 0;
        int enRetard = 0;
        int terminees = 0;
        BigDecimal sommeAvancement = BigDecimal.ZERO;
        int total = rows.size();

        List<ActiviteAvancementDTO> details = new java.util.ArrayList<>();

        LocalDate aujourdHui = LocalDate.now();

        for (UtilisateurActiviteAnneeRow r : rows) {
            String codeStatut = r.getStatutCode();
            BigDecimal avancement = r.getAvancement() == null
                    ? BigDecimal.ZERO : r.getAvancement();

            // Calcul enRetard + joursDeRetard
            long joursRetard = calculerJoursDeRetard(
                    r.getDateFinPrevue(), r.getDateFinReelle(), codeStatut);
            boolean estEnRetard = joursRetard > 0;

            // Compteurs
            if ("TERMINEE".equals(codeStatut)) {
                terminees++;
            } else if (estEnRetard) {
                enRetard++;
            } else if ("EN_COURS".equals(codeStatut)) {
                enCours++;
            } else if ("NON_COMMENCEE".equals(codeStatut)) {
                nonCommencees++;
            }

            sommeAvancement = sommeAvancement.add(avancement);

            details.add(ActiviteAvancementDTO.builder()
                    .id(r.getId())
                    .code(r.getCode())
                    .designation(r.getDesignation())
                    .reference(r.getReference())
                    .role(refRole(r.getRoleCode(), r.getRoleLibelle()))
                    .statut(refStatut(r.getStatutCode(), r.getStatutLibelle()))
                    .avancement(avancement)
                    .dateDebutPrevue(r.getDateDebutPrevue())
                    .dateFinPrevue(r.getDateFinPrevue())
                    .dateDebutReelle(r.getDateDebutReelle())
                    .dateFinReelle(r.getDateFinReelle())
                    .joursDeRetard(joursRetard)
                    .enRetard(estEnRetard)
                    .dateAffectation(r.getDateAffectation())
                    .build());
        }

        BigDecimal moyenne = total == 0 ? BigDecimal.ZERO
                : sommeAvancement.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return ResumeActivitesDTO.builder()
                .total(total)
                .nonCommencees(nonCommencees)
                .enCours(enCours)
                .enRetard(enRetard)
                .terminees(terminees)
                .avancementMoyenGlobal(moyenne)
                .details(details)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers — plans
    // ------------------------------------------------------------------

    private ResumePlansActionDTO construireResumePlans(
            List<UtilisateurPlanActionAnneeRow> rows) {

        int nonCommences = 0;
        int enCours = 0;
        int enRetard = 0;
        int termines = 0;
        BigDecimal sommeAvancement = BigDecimal.ZERO;
        int total = rows.size();

        List<PlanActionAvancementDTO> details = new java.util.ArrayList<>();

        for (UtilisateurPlanActionAnneeRow r : rows) {
            String codeStatut = r.getStatutCode();
            BigDecimal avancement = r.getAvancement() == null
                    ? BigDecimal.ZERO : r.getAvancement();

            long joursRetard = calculerJoursDeRetard(
                    r.getDateFinPrevue(), r.getDateFinReelle(), codeStatut);
            boolean estEnRetard = joursRetard > 0;

            if ("TERMINEE".equals(codeStatut)) {
                termines++;
            } else if (estEnRetard) {
                enRetard++;
            } else if ("EN_COURS".equals(codeStatut)) {
                enCours++;
            } else if (codeStatut == null || "NON_COMMENCEE".equals(codeStatut)) {
                nonCommences++;
            }

            sommeAvancement = sommeAvancement.add(avancement);

            details.add(PlanActionAvancementDTO.builder()
                    .id(r.getId())
                    .code(r.getCode())
                    .designation(r.getDesignation())
                    .role(refRole(r.getRoleCode(), r.getRoleLibelle()))
                    .statut(refStatut(r.getStatutCode(), r.getStatutLibelle()))
                    .avancement(avancement)
                    .dateDebutPrevue(r.getDateDebutPrevue())
                    .dateFinPrevue(r.getDateFinPrevue())
                    .dateDebutReelle(r.getDateDebutReelle())
                    .dateFinReelle(r.getDateFinReelle())
                    .joursDeRetard(joursRetard)
                    .enRetard(estEnRetard)
                    .dateAffectation(r.getDateAffectation())
                    .build());
        }

        BigDecimal moyenne = total == 0 ? BigDecimal.ZERO
                : sommeAvancement.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return ResumePlansActionDTO.builder()
                .total(total)
                .nonCommences(nonCommences)
                .enCours(enCours)
                .enRetard(enRetard)
                .termines(termines)
                .avancementMoyenGlobal(moyenne)
                .details(details)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers — calcul du retard
    // ------------------------------------------------------------------

    /**
     * Jours de retard selon l'etat :
     * - si terminee a temps (dateFinReelle <= dateFinPrevue)  -> 0
     * - si terminee en retard (dateFinReelle > dateFinPrevue) -> dateFinReelle - dateFinPrevue
     * - si non terminee et echeance depassee                  -> aujourd'hui - dateFinPrevue
     * - sinon                                                 -> 0
     *
     * Un statut terminal (ANNULEE, REJETEE) retourne toujours 0.
     */
    private long calculerJoursDeRetard(
            LocalDate dateFinPrevue, LocalDate dateFinReelle, String codeStatut) {

        if ("ANNULEE".equals(codeStatut) || "REJETEE".equals(codeStatut)) {
            return 0;
        }
        if (dateFinPrevue == null) {
            return 0;
        }
        if (dateFinReelle != null) {
            // Terminee : comparer les dates.
            if (!dateFinReelle.isAfter(dateFinPrevue)) {
                return 0;
            }
            return ChronoUnit.DAYS.between(dateFinPrevue, dateFinReelle);
        }
        // Non terminee : comparer aujourd'hui a l'echeance.
        LocalDate aujourdHui = LocalDate.now();
        if (!aujourdHui.isAfter(dateFinPrevue)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dateFinPrevue, aujourdHui);
    }

    // ------------------------------------------------------------------
    // Helpers — références
    // ------------------------------------------------------------------

    private ReferenceDTO refRole(String code, String libelle) {
        return code == null ? null : ReferenceDTO.builder()
                .code(code)
                .libelle(libelle)
                .build();
    }

    private ReferenceDTO refStatut(String code, String libelle) {
        return code == null ? null : ReferenceDTO.builder()
                .code(code)
                .libelle(libelle)
                .build();
    }
}