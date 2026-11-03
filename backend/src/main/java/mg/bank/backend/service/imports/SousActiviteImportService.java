package mg.bank.backend.service.imports;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.importactivite.ActiviteSousActivitesGroupDTO;
import mg.bank.backend.dto.importactivite.LivrableImportDTO;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.dto.importactivite.SousActiviteImportDTO;
import mg.bank.backend.dto.importactivite.SousActiviteImportRequest;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.activite.Livrable;
import mg.bank.backend.model.activite.SousActivite;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.activite.LivrableRepository;
import mg.bank.backend.repository.activite.SousActiviteRepository;
import mg.bank.backend.service.activite.CodeActiviteService;

@Service
@RequiredArgsConstructor
public class SousActiviteImportService {

    private final ActiviteRepository activiteRepository;
    private final SousActiviteRepository sousActiviteRepository;
    private final LivrableRepository livrableRepository;
    private final SousActiviteImportValidator validator;
    private final ImportActiviteLookup lookup;
    private final CodeActiviteService codeActiviteService;

    @Transactional
    public ResultatImportDTO importer(SousActiviteImportRequest request) {

        RapportValidationDTO rapport = validator.valider(request);
        if (!rapport.isValide()) {
            throw new ApiException(
                    "Import refusé : " + rapport.getNombreErreurs() + " erreur(s) détectée(s).",
                    HttpStatus.BAD_REQUEST);
        }

        codeActiviteService.verrouillerGeneration();

        List<String> codesGeneres = new ArrayList<>();
        int nbSous = 0;
        int nbLivrables = 0;

        for (ActiviteSousActivitesGroupDTO g : request.getActivites()) {

            Activite activite = lookup.parReference(g.getReference().trim());
            if (activite == null) {
                throw new ApiException(
                        "Activité introuvable (reference=" + g.getReference() + ").",
                        HttpStatus.NOT_FOUND);
            }

            for (SousActiviteImportDTO saDto : g.getSousActivites()) {

                boolean codeAbsent = saDto.getCode() == null || saDto.getCode().isBlank();
                String codeSous = codeAbsent
                        ? codeActiviteService.genererCodeSousActivite(activite)
                        : codeActiviteService.validerCodeSousActiviteFourni(saDto.getCode(), activite);
                if (codeAbsent) {
                    codesGeneres.add(codeSous);
                }

                SousActivite sa = sousActiviteRepository.save(SousActivite.builder()
                        .code(codeSous)
                        .designation(saDto.getDesignation())
                        .dateDebutPrevue(saDto.getDateDebut())
                        .dateFinPrevue(saDto.getDateFin())
                        .activite(activite)
                        .build());
                nbSous++;

                if (saDto.getLivrables() != null) {
                    for (LivrableImportDTO lDto : saDto.getLivrables()) {
                        livrableRepository.save(Livrable.builder()
                                .designation(lDto.getDesignation())
                                .description(lDto.getDescription())
                                .sousActivite(sa)
                                .build());
                        nbLivrables++;
                    }
                }
            }

            recalculerDatesActivite(activite);
        }

        return ResultatImportDTO.builder()
                .message("Import des sous-activités effectué avec succès.")
                .nombreActivitesImportees(0)
                .nombreSousActivitesImportees(nbSous)
                .nombreLivrablesImportes(nbLivrables)
                .nombreResultatsIntermediairesImportes(0)
                .codesGeneres(codesGeneres)
                .indicateursCrees(List.of())
                .originesCreees(List.of())
                .statutInitial(null)
                .build();
    }

    /** Recalcule les dates de l'activité à partir de TOUTES ses sous-activités. */
    private void recalculerDatesActivite(Activite activite) {
        List<SousActivite> toutes = sousActiviteRepository
                .findByActivite_IdActivite(activite.getIdActivite());

        LocalDate debut = toutes.stream()
                .map(SousActivite::getDateDebutPrevue)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo).orElse(null);
        LocalDate fin = toutes.stream()
                .map(SousActivite::getDateFinPrevue)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);

        activite.setDateDebutPrevue(debut);
        activite.setDateFinPrevue(fin);
        activiteRepository.save(activite);
    }

}