package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.ParametreResponse;
import mg.bank.backend.model.Parametre;
import mg.bank.backend.model.Utilisateur;

public final class ParametreMapper {

    private ParametreMapper() {}

    public static ParametreResponse toResponse(Parametre p) {
        if (p == null) return null;

        Utilisateur u = p.getUtilisateurModification();

        return ParametreResponse.builder()
                .id(p.getIdParametre())
                .code(p.getCode())
                .designation(p.getDesignation())
                .valeur(p.getValeur())
                .typeValeur(p.getTypeValeur())
                .description(p.getDescription())
                .categorie(p.getCategorie())
                .modifiable(p.getModifiable())
                .actif(p.getActif())
                .dateCreation(p.getDateCreation())
                .dateModification(p.getDateModification())
                .dateDesactivation(p.getDateDesactivation())
                .idUtilisateurModification(u == null ? null : u.getIdUtilisateur())
                .nomUtilisateurModification(u == null ? null : u.getPrenom() + " " + u.getNom())
                .build();
    }
}