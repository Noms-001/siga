package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.RoleResponse;
import mg.bank.backend.model.Role;

public final class RoleMapper {

    private RoleMapper() {}

    public static RoleResponse toResponse(Role r) {
        if (r == null) return null;
        return RoleResponse.builder()
                .id(r.getIdRole())
                .code(r.getCode())
                .designation(r.getLibelle())   // colonne role.designation
                .description(r.getDescription())
                .actif(r.getActif())
                .dateCreation(r.getDateCreation())
                .dateModification(r.getDateModification())
                .dateDesactivation(r.getDateDesactivation())
                .build();
    }
}