package mg.bank.backend.service.imports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.importactivite.ActiviteOriginesGroupDTO;
import mg.bank.backend.dto.importactivite.OrigineImportReferenceDTO;
import mg.bank.backend.dto.importactivite.OrigineImportRequest;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.origine.Origine;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.origine.OrigineRepository;
import mg.bank.backend.service.activite.ActiviteService;

@Service
@RequiredArgsConstructor
public class OrigineImportService {

    private static final long CLE_VERROU_IMPORT = 92_500_005L;
    private static final String TYPE_ORIGINE_IMPORT = "IMPORT";

    private final ActiviteRepository activiteRepository;
    private final OrigineRepository origineRepository;
    private final ActiviteService activiteService;
    private final OrigineImportValidator validator;
    private final ImportActiviteLookup lookup;

    @Transactional
    public ResultatImportDTO importer(OrigineImportRequest request) {

        RapportValidationDTO rapport = validator.valider(request);
        if (!rapport.isValide()) {
            throw new ApiException(
                    "Import refusé : " + rapport.getNombreErreurs() + " erreur(s) détectée(s).",
                    HttpStatus.BAD_REQUEST);
        }

        activiteRepository.verrouillerImport(CLE_VERROU_IMPORT);

        Utilisateur utilisateur = activiteService.utilisateurCourant();
        List<String> crees = new ArrayList<>();
        Map<String, Origine> cache = new HashMap<>();

        for (ActiviteOriginesGroupDTO g : request.getActivites()) {
            Activite activite = lookup.parReference(g.getReference().trim());
            if (activite == null) {
                throw new ApiException(
                        "Activité introuvable (reference=" + g.getReference() + ").",
                        HttpStatus.NOT_FOUND);
            }
            for (OrigineImportReferenceDTO dto : g.getOrigines()) {
                Origine o = resoudreOuCreer(dto.getDesignation(), utilisateur, crees, cache);
                if (activiteRepository.existsLiaisonOrigine(
                        activite.getIdActivite(), o.getIdOrigine()) == 0) {
                    activiteRepository.lierOrigine(
                            activite.getIdActivite(), o.getIdOrigine());
                }
            }
        }

        return ResultatImportDTO.builder()
                .message("Import des origines effectué avec succès.")
                .nombreActivitesImportees(0)
                .nombreSousActivitesImportees(0)
                .nombreLivrablesImportes(0)
                .nombreResultatsIntermediairesImportes(0)
                .codesGeneres(List.of())
                .indicateursCrees(List.of())
                .originesCreees(crees)
                .statutInitial(null)
                .build();
    }

    private Origine resoudreOuCreer(String designation, Utilisateur utilisateur,
                                    List<String> crees, Map<String, Origine> cache) {
        String d = designation.trim();
        String cle = d.toLowerCase(Locale.ROOT);

        Origine dejaVu = cache.get(cle);
        if (dejaVu != null) return dejaVu;

        Optional<Origine> existant = origineRepository.findByDesignationIgnoreCase(d);
        if (existant.isPresent()) {
            cache.put(cle, existant.get());
            return existant.get();
        }

        try {
            String code = genererCodeOrigine(crees.size());
            Origine cree = origineRepository.saveAndFlush(Origine.builder()
                    .code(code)
                    .designation(d)
                    .typeOrigine(TYPE_ORIGINE_IMPORT)
                    .utilisateur(utilisateur)
                    .build());
            crees.add(code);
            cache.put(cle, cree);
            return cree;
        } catch (DataIntegrityViolationException e) {
            Origine recupere = origineRepository.findByDesignationIgnoreCase(d)
                    .orElseThrow(() -> new ApiException(
                            "Impossible de créer l'origine « " + d + " ».",
                            HttpStatus.INTERNAL_SERVER_ERROR));
            cache.put(cle, recupere);
            return recupere;
        }
    }

    private String genererCodeOrigine(int offset) {
        long base = origineRepository.count();
        int n = (int) base + 1 + offset;
        String code = "ORI-" + n;
        while (origineRepository.existsByCodeIgnoreCase(code)) {
            n++;
            code = "ORI-" + n;
        }
        return code;
    }
}