package mg.bank.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.PermissionDetailResponse;
import mg.bank.backend.dto.backoffice.PermissionFiltresResponse;
import mg.bank.backend.dto.backoffice.PermissionResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.PermissionMapper;
import mg.bank.backend.repository.PermissionRepository;
import mg.bank.backend.repository.PostePermissionRepository;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PostePermissionRepository postePermissionRepository;

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
        mg.bank.backend.model.Permission permission = getEntity(id);
        List<mg.bank.backend.model.PostePermission> associations =
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
    public mg.bank.backend.model.Permission getEntity(Integer id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Permission introuvable",
                        HttpStatus.NOT_FOUND));
    }
}