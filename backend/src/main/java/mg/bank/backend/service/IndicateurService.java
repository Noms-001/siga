package mg.bank.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.IndicateurFiltresResponse;
import mg.bank.backend.dto.backoffice.IndicateurRequest;
import mg.bank.backend.dto.backoffice.IndicateurResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.IndicateurMapper;
import mg.bank.backend.model.Indicateur;
import mg.bank.backend.repository.IndicateurRepository;
import mg.bank.backend.repository.ValeurIndicateurRepository;

@Service
@RequiredArgsConstructor
public class IndicateurService {

    private final IndicateurRepository indicateurRepository;
    private final ValeurIndicateurRepository valeurIndicateurRepository; // à ajouter en haut

    /* -------------------- lecture -------------------- */

    /**
     * Liste filtrée.
     *
     * @param actif  null → tous ; true → actifs ; false → inactifs
     * @param type   filtre par type exact (nullable)
     * @param search motif de recherche sur code + designation (nullable)
     */
    @Transactional(readOnly = true)
    public List<IndicateurResponse> lister(Boolean actif, String type, String search) {
        String typeFiltre = videSiBlanc(type);
        String motif = motifRecherche(search);

        return indicateurRepository.rechercher(actif, typeFiltre, motif).stream()
                .map(IndicateurMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IndicateurResponse getById(Integer id) {
        return IndicateurMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public IndicateurFiltresResponse getFiltres() {
        return IndicateurFiltresResponse.builder()
                .types(indicateurRepository.findDistinctTypes())
                .build();
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public IndicateurResponse creer(IndicateurRequest request) {
        String code = request.getCode().trim();

        if (indicateurRepository.existsByCode(code)) {
            throw new ApiException(
                    "Un indicateur avec ce code existe déjà",
                    HttpStatus.CONFLICT);
        }

        String codeHopex = videSiBlanc(request.getCodeHopex());
        String indicateurHopex = videSiBlanc(request.getIndicateurHopex());

        /*
         * Contrôle d'unicité conditionnel : PostgreSQL accepte plusieurs
         * NULL dans une colonne UNIQUE, mais refuse deux valeurs égales.
         * On ne teste donc l'unicité que quand la valeur est fournie —
         * sinon existsByCodeHopex(null) renverrait faux (aucune ligne avec
         * codeHopex null n'est *distinctement* comparable), et le contrôle
         * serait sans effet.
         */
        if (codeHopex != null && indicateurRepository.existsByCodeHopex(codeHopex)) {
            throw new ApiException(
                    "Un indicateur avec ce code HOPEx existe déjà",
                    HttpStatus.CONFLICT);
        }
        if (indicateurHopex != null && indicateurRepository.existsByIndicateurHopex(indicateurHopex)) {
            throw new ApiException(
                    "Un indicateur avec cet indicateur HOPEx existe déjà",
                    HttpStatus.CONFLICT);
        }

        Indicateur entity = Indicateur.builder()
                .code(code)
                .designation(request.getDesignation().trim())
                .codeHopex(codeHopex)
                .indicateurHopex(indicateurHopex)
                .typeIndicateur(videSiBlanc(request.getTypeIndicateur()))
                .uniteMesure(videSiBlanc(request.getUniteMesure()))
                .frequenceVerification(videSiBlanc(request.getFrequenceVerification()))
                .frequenceAggregation(videSiBlanc(request.getFrequenceAggregation()))
                .definition(videSiBlanc(request.getDefinition()))
                .methodeDetermination(videSiBlanc(request.getMethodeDetermination()))
                .objectif(videSiBlanc(request.getObjectif()))
                .valeurCible(request.getValeurCible())
                .seuilMin(request.getSeuilMin())
                .seuilMax(request.getSeuilMax())
                .actif(request.getActif() == null ? Boolean.TRUE : request.getActif())
                .build();

        return IndicateurMapper.toResponse(indicateurRepository.save(entity));
    }

    @Transactional
    public IndicateurResponse modifier(Integer id, IndicateurRequest request) {
        Indicateur entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier un indicateur désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        String code = request.getCode().trim();

        if (indicateurRepository.existsByCodeAndIdIndicateurNot(code, id)) {
            throw new ApiException(
                    "Un indicateur avec ce code existe déjà",
                    HttpStatus.CONFLICT);
        }

        String codeHopex = videSiBlanc(request.getCodeHopex());
        String indicateurHopex = videSiBlanc(request.getIndicateurHopex());

        if (codeHopex != null
                && indicateurRepository.existsByCodeHopexAndIdIndicateurNot(codeHopex, id)) {
            throw new ApiException(
                    "Un indicateur avec ce code HOPEx existe déjà",
                    HttpStatus.CONFLICT);
        }
        if (indicateurHopex != null
                && indicateurRepository.existsByIndicateurHopexAndIdIndicateurNot(indicateurHopex, id)) {
            throw new ApiException(
                    "Un indicateur avec cet indicateur HOPEx existe déjà",
                    HttpStatus.CONFLICT);
        }

        entity.setCode(code);
        entity.setDesignation(request.getDesignation().trim());
        entity.setCodeHopex(codeHopex);
        entity.setIndicateurHopex(indicateurHopex);
        entity.setTypeIndicateur(videSiBlanc(request.getTypeIndicateur()));
        entity.setUniteMesure(videSiBlanc(request.getUniteMesure()));
        entity.setFrequenceVerification(videSiBlanc(request.getFrequenceVerification()));
        entity.setFrequenceAggregation(videSiBlanc(request.getFrequenceAggregation()));
        entity.setDefinition(videSiBlanc(request.getDefinition()));
        entity.setMethodeDetermination(videSiBlanc(request.getMethodeDetermination()));
        entity.setObjectif(videSiBlanc(request.getObjectif()));
        entity.setValeurCible(request.getValeurCible());
        entity.setSeuilMin(request.getSeuilMin());
        entity.setSeuilMax(request.getSeuilMax());

        return IndicateurMapper.toResponse(indicateurRepository.save(entity));
    }

    /* -------------------- bascule -------------------- */

    @Transactional
    public IndicateurResponse desactiver(Integer id) {
        Indicateur entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "L'indicateur est déjà désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        return IndicateurMapper.toResponse(indicateurRepository.save(entity));
    }

    @Transactional
    public IndicateurResponse activer(Integer id) {
        Indicateur entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "L'indicateur est déjà actif",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        return IndicateurMapper.toResponse(indicateurRepository.save(entity));
    }

    /* -------------------- helpers -------------------- */

    @Transactional(readOnly = true)
    public Indicateur getEntity(Integer id) {
        return indicateurRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Indicateur introuvable",
                        HttpStatus.NOT_FOUND));
    }

    private String videSiBlanc(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private String motifRecherche(String search) {
        String v = videSiBlanc(search);
        return v == null ? null : "%" + v + "%";
    }


    /* -------------------- historique des valeurs -------------------- */

    /**
     * Historique complet des valeurs d'un indicateur, du plus récent au plus
     * ancien.
     *
     * La vérification d'existence de l'indicateur distingue "aucun relevé"
     * (liste vide) de "indicateur inconnu" (404). Sans elle, un id erroné
     * renverrait une liste vide, que le front interpréterait comme un
     * indicateur sans valeur.
     */
    @Transactional(readOnly = true)
    public List<mg.bank.backend.dto.backoffice.ValeurIndicateurResponse> listerValeurs(
            Integer idIndicateur) {

        if (!indicateurRepository.existsById(idIndicateur)) {
            throw new ApiException("Indicateur introuvable", HttpStatus.NOT_FOUND);
        }

        return valeurIndicateurRepository.findHistoriqueByIndicateur(idIndicateur).stream()
                .map(mg.bank.backend.mapper.ValeurIndicateurMapper::toResponse)
                .toList();
    }
}