package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.OrigineFiltresResponse;
import mg.bank.backend.dto.backoffice.OrigineRequest;
import mg.bank.backend.dto.backoffice.OrigineResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.OrigineMapper;
import mg.bank.backend.model.Origine;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.OrigineRepository;

@Service
@RequiredArgsConstructor
public class OrigineService {

    private final OrigineRepository origineRepository;
    private final ActiviteService activiteService;

    /* -------------------- lecture -------------------- */

    /**
     * @param typesCsv liste de types en CSV (INCIDENT,RISQUE) ou null
     * @param search   motif de recherche sur code + designation, ou null
     * @param avecPlan null → tous ; true → avec plan ; false → sans plan
     * @param annee    null → toutes les années ; sinon filtre sur
     *                 l'année de date_creation
     */
    @Transactional(readOnly = true)
    public List<OrigineResponse> lister(String typesCsv, String search,
            Boolean avecPlan, Integer annee) {
        String types = (typesCsv == null || typesCsv.isBlank()) ? null : typesCsv.trim();
        return origineRepository.rechercher(types, motifRecherche(search), avecPlan, annee)
                .stream()
                .map(OrigineMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrigineResponse getById(Integer id) {
        return OrigineMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public OrigineFiltresResponse getFiltres() {
        return OrigineFiltresResponse.builder()
                .types(origineRepository.findDistinctTypes())
                .build();
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public OrigineResponse creer(OrigineRequest request) {
        String code = request.getCode().trim();

        if (origineRepository.existsByCode(code)) {
            throw new ApiException(
                    "Un signalement avec ce code existe déjà",
                    HttpStatus.CONFLICT);
        }

        Utilisateur utilisateur = activiteService.utilisateurCourant();

        Origine entity = Origine.builder()
                .code(code)
                .designation(request.getDesignation().trim())
                .typeOrigine(request.getTypeOrigine().trim().toUpperCase())
                .description(videSiBlanc(request.getDescription()))
                .dateCreation(LocalDateTime.now())
                .utilisateur(utilisateur)
                .build();

        return OrigineMapper.toResponse(origineRepository.save(entity));
    }

    /* -------------------- helpers -------------------- */

    @Transactional(readOnly = true)
    public Origine getEntity(Integer id) {
        return origineRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Signalement introuvable",
                        HttpStatus.NOT_FOUND));
    }

    private String videSiBlanc(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private String motifRecherche(String search) {
        String v = videSiBlanc(search);
        return v == null ? null : "%" + v + "%";
    }

    @Transactional(readOnly = true)
    public mg.bank.backend.dto.backoffice.SignalementDetailResponse getDetail(Integer id) {
        Origine origine = getEntity(id);
        List<mg.bank.backend.repository.projection.PlanActionOrigineRow> plans = origineRepository
                .findPlansActionByOrigine(id);
        return mg.bank.backend.mapper.OrigineMapper.toDetail(origine, plans);
    }
}