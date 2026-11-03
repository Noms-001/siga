package mg.bank.backend.service.imports;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.importactivite.ActiviteResultatsGroupDTO;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.dto.importactivite.ResultatIntermediaireImportDTO;
import mg.bank.backend.dto.importactivite.ResultatIntermediaireImportRequest;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.activite.ResultatIntermediaire;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.activite.ResultatIntermediaireRepository;

@Service
@RequiredArgsConstructor
public class ResultatIntermediaireImportService {

    private static final long CLE_VERROU_IMPORT = 92_500_003L;

    private final ActiviteRepository activiteRepository;
    private final ResultatIntermediaireRepository resultatRepository;
    private final ResultatIntermediaireImportValidator validator;
    private final ImportActiviteLookup lookup;

    @Transactional
    public ResultatImportDTO importer(ResultatIntermediaireImportRequest request) {

        RapportValidationDTO rapport = validator.valider(request);
        if (!rapport.isValide()) {
            throw new ApiException(
                    "Import refusé : " + rapport.getNombreErreurs() + " erreur(s) détectée(s).",
                    HttpStatus.BAD_REQUEST);
        }

        activiteRepository.verrouillerImport(CLE_VERROU_IMPORT);

        int nb = 0;
        for (ActiviteResultatsGroupDTO g : request.getActivites()) {
            Activite activite = lookup.parReference(g.getReference().trim());
            if (activite == null) {
                throw new ApiException(
                        "Activité introuvable (reference=" + g.getReference() + ").",
                        HttpStatus.NOT_FOUND);
            }
            for (ResultatIntermediaireImportDTO r : g.getResultatsIntermediaires()) {
                resultatRepository.save(ResultatIntermediaire.builder()
                        .designation(r.getDesignation())
                        .activite(activite)
                        .build());
                nb++;
            }
        }

        return ResultatImportDTO.builder()
                .message("Import des résultats intermédiaires effectué avec succès.")
                .nombreActivitesImportees(0)
                .nombreSousActivitesImportees(0)
                .nombreLivrablesImportes(0)
                .nombreResultatsIntermediairesImportes(nb)
                .codesGeneres(List.of())
                .indicateursCrees(List.of())
                .originesCreees(List.of())
                .statutInitial(null)
                .build();
    }
}