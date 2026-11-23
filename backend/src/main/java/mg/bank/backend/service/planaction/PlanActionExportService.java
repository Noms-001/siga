package mg.bank.backend.service.planaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.common.PlanActionExportFiltres;
import mg.bank.backend.dto.export.ActiviteLieeExportDTO;
import mg.bank.backend.dto.export.OrigineExportDTO;
import mg.bank.backend.dto.export.PlanActionExportDTO;
import mg.bank.backend.dto.export.PlanActionExportResponse;
import mg.bank.backend.dto.export.ServiceExportDTO;
import mg.bank.backend.dto.export.SuiviPlanActionExportDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.referentiel.Departement;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.planaction.PlanActionExportRepository;
import mg.bank.backend.repository.projection.planaction.ActiviteLieeExportRow;
import mg.bank.backend.repository.projection.planaction.MotifReprisePlanExportRow;
import mg.bank.backend.repository.projection.planaction.OrigineExportRow;
import mg.bank.backend.repository.projection.planaction.PlanActionExportRow;
import mg.bank.backend.repository.projection.planaction.ResponsablePlanExportRow;
import mg.bank.backend.repository.projection.planaction.SuiviPlanActionExportRow;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@Service
@RequiredArgsConstructor
public class PlanActionExportService {

    private static final String DIRECTION = "DTS";
    private static final String STATUTS_ACTIFS_CSV = "NON_COMMENCEE,EN_COURS,TERMINEE";

    private final PlanActionExportRepository planActionExportRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PlanActionExportResponse exporter(PlanActionExportFiltres filtres) {
        Utilisateur utilisateur = currentUserService.get();
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();

        if (perimetre.estSansPerimetre()) {
            throw new ApiException("Périmètre utilisateur indisponible pour l'export", HttpStatus.FORBIDDEN);
        }

        Departement departement = utilisateur.getDepartement();
        Integer serviceDemande = resoudreServiceDemande(
                filtres == null ? null : filtres.getServiceId(), perimetre, departement);
        Integer servicePerimetre = perimetre.estRattacheService() ? perimetre.getIdService() : null;
        Integer departementPerimetre = perimetre.estNiveauDepartement() ? perimetre.getIdDepartement() : null;

        String statutCode = filtres == null || estVide(filtres.getStatut()) ? null : filtres.getStatut().trim();
        String statutsParDefaut = statutCode == null ? STATUTS_ACTIFS_CSV : null;

        List<PlanActionExportRow> lignes = planActionExportRepository.findPlansPourExport(
                servicePerimetre,
                departementPerimetre,
                perimetre.getIdUtilisateurPortee(),
                perimetre.estRattacheService() ? null : serviceDemande,
                statutCode,
                statutsParDefaut,
                motif(filtres == null ? null : filtres.getSearch()),
                filtres == null ? null : filtres.getAnnee(),
                filtres == null ? null : filtres.getDateDebut(),
                filtres == null ? null : filtres.getDateFin(),
                filtres == null ? null : filtres.getOrigineId(),
                filtres == null || estVide(filtres.getTypeOrigine()) ? null : filtres.getTypeOrigine().trim(),
                filtres == null ? null : filtres.getResponsableId());

        List<Integer> ids = lignes.stream().map(PlanActionExportRow::getId).toList();

        Map<Integer, List<OrigineExportRow>> originesParPlan = ids.isEmpty()
                ? Map.of()
                : planActionExportRepository.findOriginesPourExport(ids).stream()
                        .collect(Collectors.groupingBy(
                                OrigineExportRow::getIdPlanAction,
                                LinkedHashMap::new,
                                Collectors.toList()));

        Map<Integer, List<ActiviteLieeExportRow>> activitesParPlan = ids.isEmpty()
                ? Map.of()
                : planActionExportRepository.findActivitesLieesPourExport(ids).stream()
                        .collect(Collectors.groupingBy(
                                ActiviteLieeExportRow::getIdPlanAction,
                                LinkedHashMap::new,
                                Collectors.toList()));

        Map<Integer, List<String>> responsablesParPlan = ids.isEmpty()
                ? Map.of()
                : planActionExportRepository.findResponsablesPourExport(ids).stream()
                        .collect(Collectors.groupingBy(
                                ResponsablePlanExportRow::getIdPlanAction,
                                LinkedHashMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(LinkedHashSet::new),
                                        responsables -> responsables.stream()
                                                .map(this::nomComplet)
                                                .filter(Objects::nonNull)
                                                .distinct()
                                                .toList())));

        Map<Integer, List<SuiviPlanActionExportRow>> suivisParPlan = ids.isEmpty()
                ? Map.of()
                : planActionExportRepository.findSuivisPourExport(ids).stream()
                        .collect(Collectors.groupingBy(
                                SuiviPlanActionExportRow::getIdPlanAction,
                                LinkedHashMap::new,
                                Collectors.toList()));

        Map<Integer, String> motifsParPlan = ids.isEmpty()
                ? Map.of()
                : planActionExportRepository.findMotifsReprisePourExport(ids).stream()
                        .collect(Collectors.toMap(
                                MotifReprisePlanExportRow::getIdPlanAction,
                                MotifReprisePlanExportRow::getMotif,
                                (premier, second) -> premier,
                                LinkedHashMap::new));

        List<PlanActionExportDTO> plans = new ArrayList<>();
        for (PlanActionExportRow ligne : lignes) {
            String motif = motifsParPlan.get(ligne.getId());
            List<SuiviPlanActionExportRow> suivis = suivisParPlan.getOrDefault(ligne.getId(), List.of());

            plans.add(PlanActionExportDTO.builder()
                    .id(ligne.getId())
                    .code(ligne.getCode())
                    .designation(ligne.getDesignation())
                    .dateDebutPrevue(ligne.getDateDebutPrevue())
                    .dateFinPrevue(ligne.getDateFinPrevue())
                    .dateDebutReelle(ligne.getDateDebutReelle())
                    .dateFinReelle(ligne.getDateFinReelle())
                    .statutCode(ligne.getStatutCode())
                    .statutLibelle(ligne.getStatutLibelle())
                    .avancement(valeur(ligne.getAvancement()))
                    .serviceId(ligne.getServiceId())
                    .serviceNom(serviceAffiche(ligne))
                    .servicesLies(ligne.getServicesLies())
                    .responsables(responsablesParPlan.getOrDefault(ligne.getId(), List.of()))
                    .motifReprise(motif)
                    .origines(originesParPlan.getOrDefault(ligne.getId(), List.of()).stream()
                            .map(this::versOrigine)
                            .toList())
                    .activites(activitesParPlan.getOrDefault(ligne.getId(), List.of()).stream()
                            .map(this::versActivite)
                            .toList())
                    .suivis(suivis.stream()
                            .map(suivi -> versSuivi(suivi, motif))
                            .toList())
                    .build());
        }

        String serviceNom = serviceDemande == null
                ? null
                : serviceRepository.findById(serviceDemande)
                        .map(mg.bank.backend.model.referentiel.Service::getNom)
                        .orElse(null);

        List<ServiceExportDTO> services = services(serviceDemande, serviceNom, departement, lignes);
        Integer annee = filtres != null && filtres.getAnnee() != null
                ? filtres.getAnnee()
                : LocalDate.now().getYear();

        return PlanActionExportResponse.builder()
                .direction(DIRECTION)
                .departement(departement == null ? null : departement.getNom())
                .departementCode(departement == null ? null : departement.getCode())
                .service(serviceNom)
                .serviceId(serviceDemande)
                .annee(annee)
                .dateGeneration(LocalDateTime.now())
                .services(services)
                .plans(plans)
                .build();
    }

    private Integer resoudreServiceDemande(
            Integer serviceIdDemande,
            PerimetreUtilisateur perimetre,
            Departement departement) {

        if (perimetre.estRattacheService()) {
            return perimetre.getIdService();
        }

        if (perimetre.estNiveauDepartement() && serviceIdDemande != null) {
            boolean appartient = serviceRepository.findById(serviceIdDemande)
                    .map(service -> service.getDepartement() != null
                            && service.getDepartement().getIdDepartement() != null
                            && service.getDepartement().getIdDepartement().equals(departement.getIdDepartement()))
                    .orElse(false);
            if (!appartient) {
                throw new ApiException("Service hors de votre périmètre", HttpStatus.FORBIDDEN);
            }
        }

        return serviceIdDemande;
    }

    private List<ServiceExportDTO> services(
            Integer serviceId,
            String serviceNom,
            Departement departement,
            List<PlanActionExportRow> lignes) {

        if (serviceId != null) {
            return serviceNom == null
                    ? List.of()
                    : List.of(new ServiceExportDTO(serviceId, serviceNom));
        }

        if (departement != null) {
            return serviceRepository.findByDepartementIdDepartementAndActifTrue(
                    departement.getIdDepartement()).stream()
                    .map(service -> new ServiceExportDTO(service.getIdService(), service.getNom()))
                    .toList();
        }

        Map<Integer, String> distincts = new LinkedHashMap<>();
        for (PlanActionExportRow ligne : lignes) {
            if (ligne.getServiceId() != null && ligne.getServiceNom() != null) {
                distincts.putIfAbsent(ligne.getServiceId(), ligne.getServiceNom());
            }
        }
        return distincts.entrySet().stream()
                .map(entree -> new ServiceExportDTO(entree.getKey(), entree.getValue()))
                .toList();
    }

    private String serviceAffiche(PlanActionExportRow ligne) {
        if (ligne.getServicesLies() != null && !ligne.getServicesLies().isBlank()) {
            return ligne.getServicesLies();
        }
        return ligne.getServiceNom();
    }

    private OrigineExportDTO versOrigine(OrigineExportRow ligne) {
        return OrigineExportDTO.builder()
                .code(ligne.getCode())
                .designation(ligne.getDesignation())
                .typeOrigine(ligne.getTypeOrigine())
                .estPrincipale(Boolean.TRUE.equals(ligne.getEstPrincipale()))
                .build();
    }

    private ActiviteLieeExportDTO versActivite(ActiviteLieeExportRow ligne) {
        return ActiviteLieeExportDTO.builder()
                .id(ligne.getIdActivite())
                .code(ligne.getCode())
                .designation(ligne.getDesignation())
                .serviceId(ligne.getServiceId())
                .serviceNom(ligne.getServiceNom())
                .origineCode(ligne.getOrigineCode())
                .typeOrigine(ligne.getTypeOrigine())
                .build();
    }

    private SuiviPlanActionExportDTO versSuivi(SuiviPlanActionExportRow ligne, String motif) {
        return SuiviPlanActionExportDTO.builder()
                .dateChangement(ligne.getDateChangement())
                .utilisateur(nomComplet(ligne.getPrenom(), ligne.getNom()))
                .valeurPourcentage(valeur(ligne.getValeurPourcentage()))
                .commentaire(ligne.getCommentaire())
                .motif(motif)
                .statutCode(ligne.getStatutCode())
                .statutLibelle(ligne.getStatutLibelle())
                .build();
    }

    private String nomComplet(ResponsablePlanExportRow ligne) {
        return nomComplet(ligne.getPrenom(), ligne.getNom());
    }

    private String nomComplet(String prenom, String nom) {
        String valeur = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return valeur.isEmpty() ? null : valeur;
    }

    private String motif(String recherche) {
        if (estVide(recherche)) return null;
        return "%" + recherche.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    private Double valeur(Double valeur) {
        return Objects.requireNonNullElse(valeur, 0d);
    }
}
