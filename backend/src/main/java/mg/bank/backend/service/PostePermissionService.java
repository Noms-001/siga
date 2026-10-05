package mg.bank.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.backoffice.PermissionPourPosteResponse;
import mg.bank.backend.dto.backoffice.PostePermissionRequest;
import mg.bank.backend.dto.backoffice.PostePermissionUpdateRequest;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.mapper.PermissionMapper;
import mg.bank.backend.model.Permission;
import mg.bank.backend.enums.PorteePermission;
import mg.bank.backend.model.Poste;
import mg.bank.backend.model.PostePermission;
import mg.bank.backend.model.PostePermissionId;
import mg.bank.backend.repository.PermissionRepository;
import mg.bank.backend.repository.PostePermissionRepository;
import mg.bank.backend.repository.PosteRepository;

@Service
@RequiredArgsConstructor
public class PostePermissionService {

    private final PostePermissionRepository postePermissionRepository;
    private final PosteRepository posteRepository;
    private final PermissionRepository permissionRepository;

    /* -------------------- lecture -------------------- */

    @Transactional(readOnly = true)
    public List<PermissionPourPosteResponse> lister(Integer idPoste) {
        verifierPosteExiste(idPoste);
        return postePermissionRepository.findByPosteIdWithPermission(idPoste)
                .stream()
                .map(PermissionMapper::toPosteSideResponse)
                .toList();
    }

    /* -------------------- écriture -------------------- */

    @Transactional
    public PermissionPourPosteResponse affecter(Integer idPoste, PostePermissionRequest request) {
        Poste poste = verifierPosteExiste(idPoste);
        Permission permission = verifierPermissionExiste(request.getIdPermission());

        PostePermissionId cle = new PostePermissionId(idPoste, permission.getIdPermission());

        if (postePermissionRepository.findById(cle).isPresent()) {
            throw new ApiException(
                    "Cette permission est déjà affectée à ce poste",
                    HttpStatus.CONFLICT);
        }

        verifierCombinaison(permission, request.getPortee());

        PostePermission association = PostePermission.builder()
                .id(cle)
                .poste(poste)
                .permission(permission)
                .portee(request.getPortee())
                .build();

        return PermissionMapper.toPosteSideResponse(postePermissionRepository.save(association));
    }

    @Transactional
    public PermissionPourPosteResponse modifierPortee(
            Integer idPoste,
            Integer idPermission,
            PostePermissionUpdateRequest request) {

        PostePermission association = getAssociation(idPoste, idPermission);

        verifierCombinaison(association.getPermission(), request.getPortee());

        association.setPortee(request.getPortee());

        return PermissionMapper.toPosteSideResponse(postePermissionRepository.save(association));
    }

    @Transactional
    public void retirer(Integer idPoste, Integer idPermission) {
        PostePermission association = getAssociation(idPoste, idPermission);
        postePermissionRepository.delete(association);
    }

    /* -------------------- validation -------------------- */

    /**
     * Vérifie que la combinaison (permission, portée) est autorisée.
     *
     * Aujourd'hui, seule la règle « la permission doit être active » est
     * appliquée. C'est le point d'extension unique pour toute contrainte
     * métier supplémentaire : quand les règles d'affectation auront été
     * précisées (par exemple « une permission d'action VALIDATE n'a pas de
     * sens en portée UTILISATEUR »), c'est ici qu'elles se codent — pas
     * côté frontend.
     */
    private void verifierCombinaison(Permission permission, PorteePermission portee) {

        if (Boolean.FALSE.equals(permission.getActif())) {
            throw new ApiException(
                    "Impossible d'affecter une permission désactivée",
                    HttpStatus.BAD_REQUEST);
        }

        if (portee == null) {
            throw new ApiException(
                    "La portée est obligatoire",
                    HttpStatus.BAD_REQUEST);
        }

        // À compléter : règles métier précises (permission ↔ portée).
    }

    /* -------------------- helpers -------------------- */

    private Poste verifierPosteExiste(Integer idPoste) {
        return posteRepository.findById(idPoste)
                .orElseThrow(() -> new ApiException(
                        "Poste introuvable",
                        HttpStatus.NOT_FOUND));
    }

    private Permission verifierPermissionExiste(Integer idPermission) {
        return permissionRepository.findById(idPermission)
                .orElseThrow(() -> new ApiException(
                        "Permission introuvable",
                        HttpStatus.NOT_FOUND));
    }

    private PostePermission getAssociation(Integer idPoste, Integer idPermission) {
        return postePermissionRepository.findAssociation(idPoste, idPermission)
                .orElseThrow(() -> new ApiException(
                        "Cette permission n'est pas affectée à ce poste",
                        HttpStatus.NOT_FOUND));
    }
}