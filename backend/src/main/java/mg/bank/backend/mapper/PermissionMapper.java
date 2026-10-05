package mg.bank.backend.mapper;

import java.util.List;

import mg.bank.backend.dto.backoffice.PermissionDetailResponse;
import mg.bank.backend.dto.backoffice.PermissionResponse;
import mg.bank.backend.dto.backoffice.PostePermissionResponse;
import mg.bank.backend.dto.backoffice.PermissionPourPosteResponse;
import mg.bank.backend.model.Permission;
import mg.bank.backend.model.PostePermission;

public final class PermissionMapper {

    private PermissionMapper() {}

    public static PermissionResponse toResponse(Permission p) {
        if (p == null) return null;
        return PermissionResponse.builder()
                .id(p.getIdPermission())
                .designation(p.getDesignation())
                .description(p.getDescription())
                .ressource(p.getRessource())
                .action(p.getAction())
                .actif(p.getActif())
                .dateDesactivation(p.getDateDesactivation())
                .build();
    }

    public static PermissionDetailResponse toDetail(
            Permission p,
            List<PostePermission> associations) {

        List<PostePermissionResponse> postes = associations.stream()
                .map(PermissionMapper::toPosteResponse)
                .toList();

        return PermissionDetailResponse.builder()
                .id(p.getIdPermission())
                .designation(p.getDesignation())
                .description(p.getDescription())
                .ressource(p.getRessource())
                .action(p.getAction())
                .actif(p.getActif())
                .dateDesactivation(p.getDateDesactivation())
                .postes(postes)
                .build();
    }

    private static PostePermissionResponse toPosteResponse(PostePermission pp) {
        return PostePermissionResponse.builder()
                .idPoste(pp.getPoste().getIdPoste())
                .nom(pp.getPoste().getLibelle())  // colonne poste.nom
                .portee(pp.getPortee())
                .build();
    }

    public static PermissionPourPosteResponse toPosteSideResponse(PostePermission pp) {
        if (pp == null) return null;
        var p = pp.getPermission();
        return PermissionPourPosteResponse.builder()
                .idPermission(p.getIdPermission())
                .designation(p.getDesignation())
                .ressource(p.getRessource())
                .action(p.getAction())
                .actif(p.getActif())
                .portee(pp.getPortee())
                .build();
    }
}