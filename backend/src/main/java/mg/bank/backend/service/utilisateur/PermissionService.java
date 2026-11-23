package mg.bank.backend.service.utilisateur;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.PermissionDetailResponse;
import mg.bank.backend.dto.backoffice.PermissionFiltresResponse;
import mg.bank.backend.dto.backoffice.PermissionResponse;
import mg.bank.backend.enums.PorteePermission;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.utilisateur.PermissionMapper;
import mg.bank.backend.repository.utilisateur.PermissionRepository;
import mg.bank.backend.repository.utilisateur.PostePermissionRepository;
import mg.bank.backend.security.PermissionRequirement;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PostePermissionRepository postePermissionRepository;

    /** Check an active permission assigned to the user's post. */
    @Transactional(readOnly = true)
    public boolean hasPermission(Integer idPoste, String resource, String action) {
        return postePermissionRepository.existsActivePermission(idPoste, resource, action);
    }

    /**
     * Verifie si le poste possede au moins une des permissions demandees.
     *
     * Les permissions sont alternatives : des qu'une ressource parente est
     * autorisee, l'acces a la ressource enfant est accorde.
     */
    @Transactional(readOnly = true)
    public boolean hasAnyPermission(
            Integer idPoste,
            Collection<PermissionRequirement> permissions) {

        if (permissions == null || permissions.isEmpty()) {
            return true;
        }

        return permissions.stream()
                .anyMatch(permission -> hasPermission(
                        idPoste,
                        permission.ressource(),
                        permission.action()));
    }

    /**
     * Verifie les permissions alternatives et retourne la portee effective.
     *
     * La portee la plus large est retenue : lorsqu'un endpoint autorise une
     * ressource parente et une ressource enfant, l'acces au parent etend
     * l'acces a l'enfant. Un resultat vide signifie que le poste ne possede
     * aucune des permissions demandees.
     */
    @Transactional(readOnly = true)
    public Optional<PorteePermission> autoriser(
            Integer idPoste,
            Collection<PermissionRequirement> permissions) {

        if (permissions == null || permissions.isEmpty()) {
            return Optional.empty();
        }

        List<PorteePermission> portees = new ArrayList<>();
        for (PermissionRequirement permission : permissions) {
            portees.addAll(postePermissionRepository.findPorteesActives(
                    idPoste,
                    permission.ressource(),
                    permission.action()));
        }

        if (portees.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mg.bank.backend.security.PorteeAcces.laPlusLarge(portees));
    }

    /** Effective permissions serialized for the authenticated profile. */
    @Transactional(readOnly = true)
    public List<String> permissionsFor(Integer idPoste) {
        return postePermissionRepository.findActivePermissionKeys(idPoste);
    }

    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<PermissionResponse> lister(Boolean actif, String ressource, String action) {
        String ressourceFiltre = (ressource == null || ressource.isBlank()) ? null : ressource;
        String actionFiltre = (action == null || action.isBlank()) ? null : action;

        return permissionRepository.rechercher(actif, ressourceFiltre, actionFiltre)
                .stream()
                .map(PermissionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionDetailResponse getDetail(Integer id) {
        mg.bank.backend.model.utilisateur.Permission permission = getEntity(id);
        List<mg.bank.backend.model.utilisateur.PostePermission> associations =
                postePermissionRepository.findByPermissionIdWithPoste(id);
        return PermissionMapper.toDetail(permission, associations);
    }

    @Transactional(readOnly = true)
    public PermissionFiltresResponse getFiltres() {
        return PermissionFiltresResponse.builder()
                .ressources(permissionRepository.findDistinctRessources())
                .actions(permissionRepository.findDistinctActions())
                .build();
    }

    @Transactional(readOnly = true)
    public mg.bank.backend.model.utilisateur.Permission getEntity(Integer id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Permission introuvable",
                        HttpStatus.NOT_FOUND));
    }
}