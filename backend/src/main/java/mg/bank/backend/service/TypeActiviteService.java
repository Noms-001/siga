package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.TypeActiviteRequest;
import mg.bank.backend.dto.backoffice.TypeActiviteResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.TypeActiviteMapper;
import mg.bank.backend.model.TypeActivite;
import mg.bank.backend.repository.ServiceRepository;
import mg.bank.backend.repository.TypeActiviteRepository;

@Service
@RequiredArgsConstructor
public class TypeActiviteService {

    private final TypeActiviteRepository typeActiviteRepository;
    private final ServiceRepository serviceRepository;

    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<TypeActiviteResponse> lister(Boolean actif, Integer idService) {
        return typeActiviteRepository.rechercher(actif, idService)
                .stream()
                .map(TypeActiviteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TypeActiviteResponse getById(Integer id) {
        return TypeActiviteMapper.toResponse(getEntity(id));
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public TypeActiviteResponse creer(TypeActiviteRequest request) {
        Set<mg.bank.backend.model.Service> services = resoudreServices(request.getIdServices());

        TypeActivite entity = TypeActivite.builder()
                .designation(request.getDesignation().trim())
                .description(request.getDescription())
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .dateDesactivation(null)
                .services(services)
                .build();

        return TypeActiviteMapper.toResponse(typeActiviteRepository.save(entity));
    }

    @Transactional
    public TypeActiviteResponse modifier(Integer id, TypeActiviteRequest request) {
        TypeActivite entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier un type d'activité désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setDesignation(request.getDesignation().trim());
        entity.setDescription(request.getDescription());

        synchroniserServices(entity, request.getIdServices());

        return TypeActiviteMapper.toResponse(typeActiviteRepository.save(entity));
    }

    /* -------------------- bascule -------------------- */

    @Transactional
    public TypeActiviteResponse desactiver(Integer id) {
        TypeActivite entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le type d'activité est déjà désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        entity.setDateDesactivation(LocalDateTime.now());

        return TypeActiviteMapper.toResponse(typeActiviteRepository.save(entity));
    }

    @Transactional
    public TypeActiviteResponse activer(Integer id) {
        TypeActivite entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le type d'activité est déjà actif",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        entity.setDateDesactivation(null);

        return TypeActiviteMapper.toResponse(typeActiviteRepository.save(entity));
    }

    /* -------------------- synchronisation des services -------------------- */

    /**
     * Applique un diff entre les associations actuelles et celles demandées.
     *
     * - Les services demandés et absents sont ajoutés.
     * - Les services associés mais non demandés sont retirés.
     * - Les services déjà associés et toujours demandés sont conservés tels
     *   quels (pas de suppression / réinsertion en base).
     *
     * Un `clear()` suivi d'un `addAll()` fonctionnerait, mais réécrirait toute
     * la table de jointure à chaque modification — inutile et coûteux.
     */
    private void synchroniserServices(TypeActivite entity, Set<Integer> idsDemandes) {
        Set<Integer> demandes = idsDemandes == null ? Set.of() : new HashSet<>(idsDemandes);

        // Retirer les associations non demandées
        entity.getServices().removeIf(s -> !demandes.contains(s.getIdService()));

        // Ajouter les nouvelles associations
        Set<Integer> actuels = new HashSet<>();
        for (mg.bank.backend.model.Service s : entity.getServices()) {
            actuels.add(s.getIdService());
        }

        Set<Integer> aAjouter = new HashSet<>(demandes);
        aAjouter.removeAll(actuels);

        if (!aAjouter.isEmpty()) {
            entity.getServices().addAll(resoudreServices(aAjouter));
        }
    }

    /**
     * Charge les services correspondants aux identifiants fournis.
     *
     * Refuse tout identifiant inconnu : une association avec un service
     * inexistant romprait la FK composite côté PostgreSQL, avec un message
     * d'erreur technique peu lisible pour l'utilisateur.
     */
    private Set<mg.bank.backend.model.Service> resoudreServices(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();

        List<mg.bank.backend.model.Service> trouves = serviceRepository.findAllById(ids);

        if (trouves.size() != ids.size()) {
            Set<Integer> trouvesIds = new HashSet<>();
            for (mg.bank.backend.model.Service s : trouves) {
                trouvesIds.add(s.getIdService());
            }
            Set<Integer> manquants = new HashSet<>(ids);
            manquants.removeAll(trouvesIds);

            throw new ApiException(
                    "Service(s) introuvable(s) : " + manquants,
                    HttpStatus.NOT_FOUND);
        }

        return new HashSet<>(trouves);
    }

    /* -------------------- helpers -------------------- */

    @Transactional(readOnly = true)
    public TypeActivite getEntity(Integer id) {
        return typeActiviteRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Type d'activité introuvable",
                        HttpStatus.NOT_FOUND));
    }
}