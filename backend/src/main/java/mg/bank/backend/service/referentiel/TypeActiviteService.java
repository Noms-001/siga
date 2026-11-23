package mg.bank.backend.service.referentiel;

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
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.referentiel.TypeActiviteMapper;
import mg.bank.backend.model.referentiel.TypeActivite;
import mg.bank.backend.repository.referentiel.ServiceRepository;
import mg.bank.backend.repository.referentiel.TypeActiviteRepository;
import mg.bank.backend.service.common.PorteeService;

@Service
@RequiredArgsConstructor
public class TypeActiviteService {

    private final TypeActiviteRepository typeActiviteRepository;
    private final ServiceRepository serviceRepository;
    private final PorteeService porteeService;

    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<TypeActiviteResponse> lister(Boolean actif, Integer idService) {
        Integer idServiceEffectif = porteeService.restreindreService(idService);
        PerimetreUtilisateur perimetre = porteeService.perimetreCourant();
        List<TypeActivite> types = typeActiviteRepository.rechercher(actif, idServiceEffectif);

        if (porteeService.porteeTous(perimetre)) {
            return types.stream()
                    .map(TypeActiviteMapper::toResponse)
                    .toList();
        }

        java.util.function.Predicate<mg.bank.backend.model.referentiel.Service> filtre =
                service -> porteeService.serviceAccessible(service, perimetre);

        return types.stream()
                .filter(type -> type.getServices().stream().anyMatch(filtre))
                .map(type -> TypeActiviteMapper.toResponse(type, filtre))
                .toList();
    }

    @Transactional(readOnly = true)
    public TypeActiviteResponse getById(Integer id) {
        return versResponse(getEntity(id));
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public TypeActiviteResponse creer(TypeActiviteRequest request) {
        exigerEcritureTypeActivite();
        Set<mg.bank.backend.model.referentiel.Service> services = resoudreServices(request.getIdServices());

        TypeActivite entity = TypeActivite.builder()
                .designation(request.getDesignation().trim())
                .description(request.getDescription())
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .dateDesactivation(null)
                .services(services)
                .build();

        return versResponse(typeActiviteRepository.save(entity));
    }

    @Transactional
    public TypeActiviteResponse modifier(Integer id, TypeActiviteRequest request) {
        exigerEcritureTypeActivite();
        TypeActivite entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier un type d'activité désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setDesignation(request.getDesignation().trim());
        entity.setDescription(request.getDescription());

        synchroniserServices(
                entity,
                request.getIdServices(),
                porteeService.perimetreCourant());

        return versResponse(typeActiviteRepository.save(entity));
    }

    /* -------------------- bascule -------------------- */

    @Transactional
    public TypeActiviteResponse desactiver(Integer id) {
        exigerEcritureTypeActivite();
        TypeActivite entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le type d'activité est déjà désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        entity.setDateDesactivation(LocalDateTime.now());

        return versResponse(typeActiviteRepository.save(entity));
    }

    @Transactional
    public TypeActiviteResponse activer(Integer id) {
        exigerEcritureTypeActivite();
        TypeActivite entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le type d'activité est déjà actif",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        entity.setDateDesactivation(null);

        return versResponse(typeActiviteRepository.save(entity));
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
    private void synchroniserServices(
            TypeActivite entity,
            Set<Integer> idsDemandes,
            PerimetreUtilisateur perimetre) {

        Set<Integer> demandes = idsDemandes == null ? Set.of() : new HashSet<>(idsDemandes);

        // Retirer les associations non demandées, sans toucher aux services
        // hors périmètre : ils restent gérés par leur propre périmètre.
        entity.getServices().removeIf(s -> !demandes.contains(s.getIdService())
                && porteeService.serviceAccessible(s, perimetre));

        // Ajouter les nouvelles associations
        Set<Integer> actuels = new HashSet<>();
        for (mg.bank.backend.model.referentiel.Service s : entity.getServices()) {
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
    private Set<mg.bank.backend.model.referentiel.Service> resoudreServices(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();

        List<mg.bank.backend.model.referentiel.Service> trouves = serviceRepository.findAllById(ids);

        if (trouves.size() != ids.size()) {
            Set<Integer> trouvesIds = new HashSet<>();
            for (mg.bank.backend.model.referentiel.Service s : trouves) {
                trouvesIds.add(s.getIdService());
            }
            Set<Integer> manquants = new HashSet<>(ids);
            manquants.removeAll(trouvesIds);

            throw new ApiException(
                    "Service(s) introuvable(s) : " + manquants,
                    HttpStatus.NOT_FOUND);
        }

        trouves.forEach(porteeService::exigerService);
        return new HashSet<>(trouves);
    }

    /* -------------------- helpers -------------------- */

    private TypeActiviteResponse versResponse(TypeActivite type) {
        PerimetreUtilisateur perimetre = porteeService.perimetreCourant();
        return TypeActiviteMapper.toResponse(
                type,
                service -> porteeService.serviceAccessible(service, perimetre));
    }

    private void exigerEcritureTypeActivite() {
        if (!porteeService.porteeServiceOuPlus()) {
            throw new ApiException(
                    "La gestion des types d'activité nécessite une portée service ou supérieure",
                    HttpStatus.FORBIDDEN);
        }
    }

    @Transactional(readOnly = true)
    public TypeActivite getEntity(Integer id) {
        TypeActivite type = typeActiviteRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Type d'activité introuvable",
                        HttpStatus.NOT_FOUND));
        if (!porteeService.porteeTous()
                && type.getServices().stream().noneMatch(porteeService::serviceAccessible)) {
            throw new ApiException("Type d'activité introuvable", HttpStatus.NOT_FOUND);
        }
        return type;
    }
}