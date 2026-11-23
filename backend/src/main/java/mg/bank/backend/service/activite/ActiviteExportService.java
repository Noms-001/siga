package mg.bank.backend.service.activite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.ActiviteFiltreCriteria;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.dto.export.ActiviteExportDTO;
import mg.bank.backend.dto.export.ActiviteExportResponse;
import mg.bank.backend.dto.export.ServiceExportDTO;
import mg.bank.backend.dto.export.SousActiviteExportDTO;
import mg.bank.backend.dto.export.SuiviExportDTO;
import mg.bank.backend.model.referentiel.Departement;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.projection.activite.ActiviteExportRow;
import mg.bank.backend.repository.projection.activite.ResponsableExportRow;
import mg.bank.backend.repository.projection.activite.SousActiviteExportRow;
import mg.bank.backend.repository.projection.activite.SuiviExportRow;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.service.common.ActiviteQuerySupport;
import mg.bank.backend.service.utilisateur.CurrentUserService;

@Service
@RequiredArgsConstructor
public class ActiviteExportService {

    private static final String DIRECTION = "DTS";

    private final ActiviteRepository activiteRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public ActiviteExportResponse exporter(ActiviteFiltreCriteria filtres) {

        Utilisateur utilisateur = currentUserService.get();
        PerimetreUtilisateur perimetre = currentUserService.getPerimetre();
        var args = ActiviteQuerySupport.arguments(filtres, perimetre);

        List<ActiviteExportRow> lignes = activiteRepository.findActivitesPourExport(
                args.pta(),
                args.recherche(),
                args.objectifId(),
                args.objectifRecherche(),
                args.servicePerimetre(),
                args.departementPerimetre(), args.idUtilisateurPortee(),
                args.serviceDemande(),
                args.prioriteId(),
                args.typeActiviteId(),
                args.siteId(),
                args.statutCode(),
                args.codesTerminaux(),
                args.annee(),
                args.moisDebut(),
                args.moisFin(),
                args.dateDebut(),
                args.dateFin());

        List<Integer> ids = lignes.stream()
                .map(ActiviteExportRow::getId)
                .toList();

        List<SousActiviteExportRow> sousActivites = ids.isEmpty()
                ? List.of()
                : activiteRepository.findSousActivitesPourExport(ids);

        List<SuiviExportRow> suivis = ids.isEmpty()
                ? List.of()
                : activiteRepository.findSuivisPourExport(ids);

        List<ResponsableExportRow> responsablesActivites = ids.isEmpty()
                ? List.of()
                : activiteRepository.findResponsablesActivitesPourExport(ids);

        List<ResponsableExportRow> responsablesSousActivites = ids.isEmpty()
                ? List.of()
                : activiteRepository.findResponsablesSousActivitesPourExport(ids);

        Map<Integer, List<SousActiviteExportRow>> sousParActivite = sousActivites.stream()
                .collect(Collectors.groupingBy(
                        SousActiviteExportRow::getIdActivite,
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<Integer, List<SuiviExportRow>> suivisParSousActivite = suivis.stream()
                .collect(Collectors.groupingBy(
                        SuiviExportRow::getIdSousActivite,
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<Integer, List<String>> responsablesParActivite = new LinkedHashMap<>();
        for (ResponsableExportRow responsable : responsablesActivites) {
            responsablesParActivite
                    .computeIfAbsent(responsable.getIdActivite(), cle -> new ArrayList<>())
                    .add(nomComplet(responsable));
        }

        Map<Integer, List<String>> responsablesParSousActivite = new LinkedHashMap<>();
        for (ResponsableExportRow responsable : responsablesSousActivites) {
            responsablesParSousActivite
                    .computeIfAbsent(responsable.getIdSousActivite(), cle -> new ArrayList<>())
                    .add(nomComplet(responsable));
        }

        List<ActiviteExportDTO> activites = new ArrayList<>();

        for (ActiviteExportRow ligne : lignes) {
            List<SousActiviteExportRow> sousLignes = sousParActivite.getOrDefault(
                    ligne.getId(), List.of());

            LinkedHashSet<String> responsables = new LinkedHashSet<>(
                    responsablesParActivite.getOrDefault(ligne.getId(), List.of()));

            List<SousActiviteExportDTO> sousDtos = new ArrayList<>();

            for (SousActiviteExportRow sousLigne : sousLignes) {
                List<String> responsablesSous = responsablesParSousActivite
                        .getOrDefault(sousLigne.getIdSousActivite(), List.of());
                responsables.addAll(responsablesSous);

                List<SuiviExportDTO> suiviDtos = suivisParSousActivite
                        .getOrDefault(sousLigne.getIdSousActivite(), List.of())
                        .stream()
                        .map(this::versSuivi)
                        .toList();

                String statutCode = sousLigne.getStatutCode() != null
                        ? sousLigne.getStatutCode()
                        : ligne.getStatutCode();
                String statutLibelle = sousLigne.getStatutLibelle() != null
                        ? sousLigne.getStatutLibelle()
                        : ligne.getStatutLibelle();

                sousDtos.add(SousActiviteExportDTO.builder()
                        .id(sousLigne.getIdSousActivite())
                        .code(sousLigne.getCode())
                        .designation(sousLigne.getDesignation())
                        .dateDebutPrevue(sousLigne.getDateDebutPrevue())
                        .dateFinPrevue(sousLigne.getDateFinPrevue())
                        .statutCode(statutCode)
                        .statutLibelle(statutLibelle)
                        .responsables(responsablesSous)
                        .avancement(valeur(sousLigne.getAvancement()))
                        .suivis(suiviDtos)
                        .build());
            }

            activites.add(ActiviteExportDTO.builder()
                    .id(ligne.getId())
                    .code(ligne.getCode())
                    .reference(ligne.getReference())
                    .designation(ligne.getDesignation())
                    .pta(Boolean.TRUE.equals(ligne.getPta()))
                    .objectifSpecifiqueCode(ligne.getObjectifCode())
                    .objectifSpecifiqueDesignation(ligne.getObjectifDesignation())
                    .procedure(ligne.getProcedureLibelle())
                    .dateDebutPrevue(ligne.getDateDebutPrevue())
                    .dateFinPrevue(ligne.getDateFinPrevue())
                    .statutCode(ligne.getStatutCode())
                    .statutLibelle(ligne.getStatutLibelle())
                    .serviceId(ligne.getServiceId())
                    .serviceNom(ligne.getServiceNom())
                    .priorite(ligne.getPrioriteLibelle())
                    .responsables(new ArrayList<>(responsables))
                    .avancement(valeur(ligne.getAvancement()))
                    .sousActivites(sousDtos)
                    .build());
        }

        Departement departement = utilisateur.getDepartement();
        Integer serviceId = perimetre.estRattacheService()
                ? perimetre.getIdService()
                : (filtres == null ? null : filtres.getServiceId());

        String serviceNom = serviceId == null
                ? null
                : serviceRepository.findById(serviceId)
                        .map(mg.bank.backend.model.referentiel.Service::getNom)
                        .orElse(null);

        List<ServiceExportDTO> services = services(
                serviceId, serviceNom, departement, lignes);

        Integer annee = filtres != null && filtres.getAnnee() != null
                ? filtres.getAnnee()
                : LocalDate.now().getYear();

        return ActiviteExportResponse.builder()
                .direction(DIRECTION)
                .departement(departement == null ? null : departement.getNom())
                .departementCode(departement == null ? null : departement.getCode())
                .service(serviceNom)
                .serviceId(serviceId)
                .annee(annee)
                .dateGeneration(LocalDateTime.now())
                .services(services)
                .activites(activites)
                .build();
    }

    private List<ServiceExportDTO> services(
            Integer serviceId,
            String serviceNom,
            Departement departement,
            List<ActiviteExportRow> lignes) {

        if (serviceId != null) {
            return serviceNom == null
                    ? List.of()
                    : List.of(new ServiceExportDTO(serviceId, serviceNom));
        }

        if (departement != null) {
            return serviceRepository
                    .findByDepartementIdDepartementAndActifTrue(
                            departement.getIdDepartement())
                    .stream()
                    .map(service -> new ServiceExportDTO(
                            service.getIdService(), service.getNom()))
                    .toList();
        }

        Map<Integer, String> distincts = new LinkedHashMap<>();
        for (ActiviteExportRow ligne : lignes) {
            if (ligne.getServiceId() != null && ligne.getServiceNom() != null) {
                distincts.putIfAbsent(ligne.getServiceId(), ligne.getServiceNom());
            }
        }

        return distincts.entrySet().stream()
                .map(entree -> new ServiceExportDTO(entree.getKey(), entree.getValue()))
                .toList();
    }

    private SuiviExportDTO versSuivi(SuiviExportRow ligne) {
        return SuiviExportDTO.builder()
                .dateChangement(ligne.getDateChangement())
                .utilisateur(nomComplet(
                        ligne.getUtilisateurPrenom(),
                        ligne.getUtilisateurNom()))
                .valeurPourcentage(valeur(ligne.getValeurPourcentage()))
                .commentaire(ligne.getCommentaire())
                .motif(ligne.getMotif())
                .statutCode(ligne.getStatutCode())
                .statutLibelle(ligne.getStatutLibelle())
                .build();
    }

    private String nomComplet(ResponsableExportRow ligne) {
        return nomComplet(ligne.getPrenom(), ligne.getNom());
    }

    private String nomComplet(String prenom, String nom) {
        String valeur = ((prenom == null ? "" : prenom) + " "
                + (nom == null ? "" : nom)).trim();
        return valeur.isEmpty() ? null : valeur;
    }

    private Double valeur(Double valeur) {
        return Objects.requireNonNullElse(valeur, 0d);
    }
}
