package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.RoleRequest;
import mg.bank.backend.dto.backoffice.RoleResponse;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.RoleMapper;
import mg.bank.backend.model.Role;
import mg.bank.backend.repository.RoleRepository;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<RoleResponse> lister(boolean inclureInactifs) {
        List<Role> roles = inclureInactifs
                ? roleRepository.findAll()
                : roleRepository.findByActifTrue();

        return roles.stream().map(RoleMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Integer id) {
        return RoleMapper.toResponse(getEntity(id));
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public RoleResponse creer(RoleRequest request) {
        String code = request.getCode().trim().toUpperCase();

        if (roleRepository.existsByCode(code)) {
            throw new ApiException(
                    "Un rôle avec ce code existe déjà",
                    HttpStatus.CONFLICT);
        }

        Role entity = Role.builder()
                .code(code)
                .libelle(request.getDesignation().trim())
                .description(request.getDescription())
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .dateModification(null)
                .dateDesactivation(null)
                .build();

        return RoleMapper.toResponse(roleRepository.save(entity));
    }

    @Transactional
    public RoleResponse modifier(Integer id, RoleRequest request) {
        Role entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Impossible de modifier un rôle désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        String code = request.getCode().trim().toUpperCase();

        if (roleRepository.existsByCodeAndIdRoleNot(code, id)) {
            throw new ApiException(
                    "Un rôle avec ce code existe déjà",
                    HttpStatus.CONFLICT);
        }

        entity.setCode(code);
        entity.setLibelle(request.getDesignation().trim());
        entity.setDescription(request.getDescription());
        entity.setDateModification(LocalDateTime.now());

        return RoleMapper.toResponse(roleRepository.save(entity));
    }

    /* -------------------- bascule -------------------- */

    @Transactional
    public RoleResponse desactiver(Integer id) {
        Role entity = getEntity(id);

        if (Boolean.FALSE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le rôle est déjà désactivé",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(false);
        entity.setDateDesactivation(LocalDateTime.now());
        entity.setDateModification(LocalDateTime.now());

        return RoleMapper.toResponse(roleRepository.save(entity));
    }

    @Transactional
    public RoleResponse activer(Integer id) {
        Role entity = getEntity(id);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new ApiException(
                    "Le rôle est déjà actif",
                    HttpStatus.BAD_REQUEST);
        }

        entity.setActif(true);
        entity.setDateDesactivation(null);
        entity.setDateModification(LocalDateTime.now());

        return RoleMapper.toResponse(roleRepository.save(entity));
    }

    /* -------------------- helpers -------------------- */

    @Transactional(readOnly = true)
    public Role getEntity(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "Rôle introuvable",
                        HttpStatus.NOT_FOUND));
    }
}