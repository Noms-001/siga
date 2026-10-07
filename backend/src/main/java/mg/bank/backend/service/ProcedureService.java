package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.EtapeValidationRequest;
import mg.bank.backend.dto.backoffice.ProcedureRequest;
import mg.bank.backend.dto.backoffice.ProcedureResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.ProcedureMapper;
import mg.bank.backend.model.EtapeValidation;
import mg.bank.backend.model.Poste;
import mg.bank.backend.model.Procedure;
import mg.bank.backend.repository.PosteRepository;
import mg.bank.backend.repository.ProcedureRepository;

@Service
@RequiredArgsConstructor
public class ProcedureService {

    private final ProcedureRepository procedureRepository;
    private final PosteRepository posteRepository;

    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<ProcedureResponse> lister(boolean inclureInactifs) {
        return (inclureInactifs
                ? procedureRepository.findAll()
                : procedureRepository.findByActifTrue())
                .stream()
                .map(ProcedureMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProcedureResponse getById(Integer id) {
        return ProcedureMapper.toResponse(getEntity(id));
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public ProcedureResponse creer(ProcedureRequest request) {
        String designation = request.getDesignation().trim();

        if (procedureRepository.existsByDesignationIgnoreCase(designation)) {
            throw new ApiException(
                    "Une procédure avec cette désignation existe déjà",
                    HttpStatus.CONFLICT);
        }

        Procedure entity = Procedure.builder()
                .designation(designation)
                .description(request.getDescription())
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .dateModification(null)
                .dateDesactivation(null)
                .etapesValidation(new ArrayList<>())
                .build();

        appliquerEtapes(entity, request.getEtapes());

        return ProcedureMapper.toResponse(procedureRepository.save(entity));
    }

    @Transactional
    public ProcedureResponse modifier(Integer id, ProcedureRequest request) {
        Procedure entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier une procédure désactivée",
                    HttpStatus.BAD_REQUEST);
        }

        String designation = request.getDesignation().trim();

        if (procedureRepository.existsByDesignationIgnoreCaseAndIdProcedureNot(designation, id)) {
            throw new ApiException(
                    "Une procédure avec cette désignation existe déjà",
                    HttpStatus.CONFLICT);
        }

        entity.setDesignation(designation);
        entity.setDescription(request.getDescription());
        entity.setDateModification(LocalDateTime.now());

        appliquerEtapes(entity, request.getEtapes());

        return ProcedureMapper.toResponse(procedureRepository.save(entity));
    }

    /**
     * Applique la liste d'étapes demandée à la procédure.
     *
     * - les étapes avec un id existant sont mises à jour et réactivées ;
     * - les étapes sans id sont créées ;
     * - les étapes absentes de la requête sont DÉSACTIVÉES (actif = false,
     * dateDesactivation = maintenant), jamais supprimées : une étape a pu
     * servir dans une validation existante, et la supprimer violerait la
     * contrainte de clé étrangère posée par validation_activite ;
     * - le `niveau` est réécrit selon l'ordre de la liste reçue — c'est
     * l'ordre du formulaire qui fait foi, les étapes désactivées n'y
     * participent pas.
     *
     * Le `@Where(clause = "actif = true")` sur la collection garantit que
     * les étapes désactivées n'apparaissent plus dans les lectures suivantes.
     */
    private void appliquerEtapes(Procedure entity, List<EtapeValidationRequest> demandes) {
        if (demandes == null)
            demandes = List.of();

        Map<Integer, EtapeValidation> existantesParId = entity.getEtapesValidation().stream()
                .filter(e -> e.getIdEtapeValidation() != null)
                .collect(Collectors.toMap(EtapeValidation::getIdEtapeValidation, Function.identity()));

        Set<Integer> idsConserves = new HashSet<>();
        List<EtapeValidation> nouvelles = new ArrayList<>();

        for (int i = 0; i < demandes.size(); i++) {
            EtapeValidationRequest req = demandes.get(i);
            int niveau = i + 1;

            EtapeValidation etape;
            if (req.getId() != null) {
                etape = existantesParId.get(req.getId());
                if (etape == null) {
                    throw new ApiException(
                            "Étape introuvable : id " + req.getId(),
                            HttpStatus.NOT_FOUND);
                }
                idsConserves.add(etape.getIdEtapeValidation());
            } else {
                etape = new EtapeValidation();
                etape.setProcedure(entity);
            }

            // Une étape demandée est par définition active : qu'elle soit nouvelle
            // ou qu'elle ait été désactivée plus tôt, elle reprend du service.
            etape.setActif(true);
            etape.setDateDesactivation(null);

            etape.setDesignation(req.getDesignation().trim());
            etape.setDescription(req.getDescription());
            etape.setNiveau(niveau);
            etape.setObligatoire(req.getObligatoire());
            etape.setRetour(req.getRetour());
            etape.setPostesDecideurs(resoudrePostes(req.getIdPostesDecideurs()));

            nouvelles.add(etape);
        }

        /*
         * Désactivation des étapes retirées.
         *
         * On marque actif = false sans les retirer de la collection : laisser
         * Hibernate détacher l'entité (par remove) ne produirait pas le UPDATE
         * attendu sur une entité hors session. La désactivation est explicite,
         * et @Where les masquera au prochain chargement.
         *
         * On ne désactive que celles qui étaient actives : une étape déjà
         * désactivée depuis un précédent passage ne doit pas changer de
         * date_desactivation.
         */
        for (EtapeValidation existante : entity.getEtapesValidation()) {
            if (existante.getIdEtapeValidation() != null
                    && !idsConserves.contains(existante.getIdEtapeValidation())
                    && Boolean.TRUE.equals(existante.getActif())) {
                existante.setActif(false);
                existante.setDateDesactivation(LocalDateTime.now());
            }
        }

        // Ajout des nouvelles étapes à la collection (les mises à jour sont déjà
        // sur les instances existantes).
        for (EtapeValidation e : nouvelles) {
            if (!entity.getEtapesValidation().contains(e)) {
                entity.getEtapesValidation().add(e);
            }
        }

        // Tri par niveau — cohérent avec ce qui sera relu.
        entity.getEtapesValidation().sort(
                Comparator.comparing(EtapeValidation::getNiveau,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private List<Poste> resoudrePostes(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return new ArrayList<>();

        List<Poste> trouves = posteRepository.findAllById(ids);

        if (trouves.size() != ids.size()) {
            Set<Integer> trouvesIds = trouves.stream()
                    .map(Poste::getIdPoste)
                    .collect(Collectors.toSet());
            Set<Integer> manquants = new HashSet<>(ids);
            manquants.removeAll(trouvesIds);

            throw new ApiException(
                    "Poste(s) introuvable(s) : " + manquants,
                    HttpStatus.NOT_FOUND);
        }

        return new ArrayList<>(trouves);
    }

    /* -------------------- bascule -------------------- */

    @Transactional
    public ProcedureResponse desactiver(Integer id) {
        Procedure entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "La procédure est déjà désactivée",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        entity.setDateDesactivation(LocalDateTime.now());
        entity.setDateModification(LocalDateTime.now());

        return ProcedureMapper.toResponse(procedureRepository.save(entity));
    }

    @Transactional
    public ProcedureResponse activer(Integer id) {
        Procedure entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "La procédure est déjà active",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        entity.setDateDesactivation(null);
        entity.setDateModification(LocalDateTime.now());

        return ProcedureMapper.toResponse(procedureRepository.save(entity));
    }

    /* -------------------- helpers -------------------- */

    @Transactional(readOnly = true)
    public Procedure getEntity(Integer id) {
        return procedureRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Procédure introuvable",
                        HttpStatus.NOT_FOUND));
    }
}