package mg.bank.backend.service;

import java.time.LocalDate;
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
import mg.bank.backend.repository.OrigineRepository;

@Service
@RequiredArgsConstructor
public class OrigineService {

    private final OrigineRepository origineRepository;
    private final ActiviteService activiteService;
    private static final int LONGUEUR_SEQUENCE = 3;

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
        String type = request.getTypeOrigine().trim().toUpperCase();

        Origine entity = Origine.builder()
                .code(prochainCode(type))
                .designation(request.getDesignation().trim())
                .typeOrigine(type)
                .description(videSiBlanc(request.getDescription()))
                .dateCreation(LocalDateTime.now())
                .utilisateur(activiteService.utilisateurCourant())
                .build();

        try {
            return OrigineMapper.toResponse(origineRepository.save(entity));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Le seul cas où l'INSERT peut échouer ici est un conflit sur
            // `code` (toutes les autres colonnes sont validées en amont, ou
            // nullable). On traduit en 409 — l'utilisateur n'a rien à faire
            // de la trace SQL.
            throw new ApiException(
                    "Un signalement avec ce code vient d'être créé. "
                            + "Rechargez la page pour obtenir un code à jour.",
                    HttpStatus.CONFLICT);
        }
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

    /**
     * Code proposé pour un nouveau signalement du type demandé.
     *
     * Le code est calculé à la volée à partir du dernier numéro utilisé
     * dans la base pour ce préfixe et cette année. Il est appelé par le
     * frontend à chaque changement de type, pour affichage dans un champ
     * en lecture seule — c'est un APERÇU, pas une réservation.
     *
     * Deux appels concurrents pour le même type avant toute création
     * renverront la même proposition. Le second enregistrement qui suivra
     * sera refusé par la contrainte UNIQUE(code), ce qui reste une erreur
     * de saisie (double clic, onglet dupliqué) et non une panne.
     */
    @Transactional(readOnly = true)
    public String prochainCode(String typeOrigine) {
        if (typeOrigine == null || typeOrigine.isBlank()) {
            throw new ApiException(
                    "Le type est obligatoire pour proposer un code",
                    HttpStatus.BAD_REQUEST);
        }

        String prefix = prefixPour(typeOrigine);
        int annee = LocalDate.now().getYear();

        String extractPattern = "^" + prefix + "-" + annee + "-([0-9]+)$";
        String matchPattern = "^" + prefix + "-" + annee + "-[0-9]+$";

        int dernier = origineRepository.dernierNumeroPour(extractPattern, matchPattern);
        int suivant = dernier + 1;

        return prefix + "-" + annee + "-" + String.format("%0" + LONGUEUR_SEQUENCE + "d", suivant);
    }

    /**
     * Préfixe du code pour un type de signalement.
     *
     * Trois types nommés (INCIDENT, RISQUE, AUTRE) ont un préfixe explicite.
     * Tout autre type tombe sur ses trois premières lettres, en majuscules —
     * si le référentiel des origines gagne un jour un type « OBSERVATION »,
     * son code sera `OBS-2026-001` sans qu'il faille modifier le code Java.
     *
     * Le repli évite un 500 sur un type inconnu, et la contrainte UNIQUE sur
     * `code` reste la garantie d'unicité, pas le format.
     */
    private String prefixPour(String typeOrigine) {
        String t = typeOrigine.trim().toUpperCase();
        return switch (t) {
            case "INCIDENT" -> "INC";
            case "RISQUE" -> "RSQ";
            case "AUTRE" -> "AUT";
            default -> t.substring(0, Math.min(3, t.length()));
        };
    }

    
}