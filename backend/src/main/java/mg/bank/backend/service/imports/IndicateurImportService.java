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
import mg.bank.backend.dto.importactivite.ActiviteIndicateursGroupDTO;
import mg.bank.backend.dto.importactivite.IndicateurImportReferenceDTO;
import mg.bank.backend.dto.importactivite.IndicateurImportRequest;
import mg.bank.backend.dto.importactivite.RapportValidationDTO;
import mg.bank.backend.dto.importactivite.ResultatImportDTO;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.indicateur.Indicateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.indicateur.IndicateurRepository;

@Service
@RequiredArgsConstructor
public class IndicateurImportService {

    private static final long CLE_VERROU_IMPORT = 92_500_004L;

    private final ActiviteRepository activiteRepository;
    private final IndicateurRepository indicateurRepository;
    private final IndicateurImportValidator validator;
    private final ImportActiviteLookup lookup;

    @Transactional
    public ResultatImportDTO importer(IndicateurImportRequest request) {

        RapportValidationDTO rapport = validator.valider(request);
        if (!rapport.isValide()) {
            throw new ApiException(
                    "Import refusé : " + rapport.getNombreErreurs() + " erreur(s) détectée(s).",
                    HttpStatus.BAD_REQUEST);
        }

        activiteRepository.verrouillerImport(CLE_VERROU_IMPORT);

        List<String> crees = new ArrayList<>();
        Map<String, Indicateur> cache = new HashMap<>();
        int nbLiaisons = 0;

        for (ActiviteIndicateursGroupDTO g : request.getActivites()) {
            Activite activite = lookup.parReference(g.getReference().trim());
            if (activite == null) {
                throw new ApiException(
                        "Activité introuvable (reference=" + g.getReference() + ").",
                        HttpStatus.NOT_FOUND);
            }
            for (IndicateurImportReferenceDTO dto : g.getIndicateurs()) {
                Indicateur ind = resoudreOuCreer(dto, crees, cache);
                if (activiteRepository.existsLiaison(
                        activite.getIdActivite(), ind.getIdIndicateur()) == 0) {
                    activiteRepository.lierIndicateur(
                            activite.getIdActivite(), ind.getIdIndicateur());
                    nbLiaisons++;
                }
            }
        }

        return ResultatImportDTO.builder()
                .message("Import des indicateurs effectué avec succès.")
                .nombreActivitesImportees(0)
                .nombreSousActivitesImportees(0)
                .nombreLivrablesImportes(0)
                .nombreResultatsIntermediairesImportes(0)
                .codesGeneres(List.of())
                .indicateursCrees(crees)
                .originesCreees(List.of())
                .statutInitial(null)
                .build();
    }

    private Indicateur resoudreOuCreer(IndicateurImportReferenceDTO dto,
                                       List<String> crees,
                                       Map<String, Indicateur> cache) {
        String d = dto.getDesignation().trim();
        String cle = d.toLowerCase(Locale.ROOT);

        Indicateur dejaVu = cache.get(cle);
        if (dejaVu != null) return dejaVu;

        Optional<Indicateur> existant = indicateurRepository.findByDesignationIgnoreCase(d);
        if (existant.isPresent()) {
            cache.put(cle, existant.get());
            return existant.get();
        }

        try {
            String code = genererCodeIndicateur(crees.size());
            Indicateur cree = indicateurRepository.saveAndFlush(Indicateur.builder()
                    .code(code)
                    .designation(d)
                    .typeIndicateur(emptyToNull(dto.getTypeIndicateur()))
                    .uniteMesure(emptyToNull(dto.getUniteMesure()))
                    .frequenceVerification(emptyToNull(dto.getFrequenceVerification()))
                    .frequenceAggregation(emptyToNull(dto.getFrequenceAggregation()))
                    .definition(emptyToNull(dto.getDefinition()))
                    .methodeDetermination(emptyToNull(dto.getMethodeDetermination()))
                    .objectif(emptyToNull(dto.getObjectif()))
                    .valeurCible(dto.getValeurCible())
                    .seuilMin(dto.getSeuilMin())
                    .seuilMax(dto.getSeuilMax())
                    .actif(Boolean.TRUE)
                    .build());
            crees.add(code);
            cache.put(cle, cree);
            return cree;
        } catch (DataIntegrityViolationException e) {
            Indicateur recupere = indicateurRepository.findByDesignationIgnoreCase(d)
                    .orElseThrow(() -> new ApiException(
                            "Impossible de créer l'indicateur « " + d + " ».",
                            HttpStatus.INTERNAL_SERVER_ERROR));
            cache.put(cle, recupere);
            return recupere;
        }
    }

    private String genererCodeIndicateur(int offset) {
        long base = indicateurRepository.count();
        int n = (int) base + 1 + offset;
        String code = "IND-" + n;
        while (indicateurRepository.existsByCodeIgnoreCase(code)) {
            n++;
            code = "IND-" + n;
        }
        return code;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}