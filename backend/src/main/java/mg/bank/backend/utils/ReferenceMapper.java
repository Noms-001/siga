package mg.bank.backend.utils;

import mg.bank.backend.dto.common.ReferenceDTO;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.planaction.PlanAction;
import mg.bank.backend.model.referentiel.ObjectifSpecifique;
import mg.bank.backend.model.referentiel.Priorite;
import mg.bank.backend.model.referentiel.Site;
import mg.bank.backend.model.referentiel.Statut;
import mg.bank.backend.model.referentiel.TypeActivite;
import mg.bank.backend.model.utilisateur.Role;
import mg.bank.backend.model.utilisateur.Utilisateur;


public final class ReferenceMapper {

    private ReferenceMapper() {
    }

    /* -------------------- depuis les entites -------------------- */

    public static ReferenceDTO fromUtilisateur(Utilisateur u) {
        if (u == null) return null;
        return ReferenceDTO.builder()
                .id(u.getIdUtilisateur())
                .libelle(u.getPrenom() + " " + u.getNom())
                .build();
    }

    public static ReferenceDTO fromActivite(Activite a) {
        if (a == null) return null;
        return ReferenceDTO.builder()
                .id(a.getIdActivite())
                .code(a.getCode())
                .libelle(a.getDesignation())
                .build();
    }

    public static ReferenceDTO fromPlanAction(PlanAction p) {
        if (p == null) return null;
        return ReferenceDTO.builder()
                .id(p.getIdPlanAction())
                .code(p.getCode())
                .libelle(p.getDesignation())
                .build();
    }

    public static ReferenceDTO fromRole(Role r) {
        if (r == null) return null;
        return ReferenceDTO.builder()
                .id(r.getIdRole())
                .code(r.getCode())
                .libelle(r.getLibelle())
                .build();
    }

    public static ReferenceDTO fromStatut(Statut s) {
        if (s == null) return null;
        return ReferenceDTO.builder()
                .id(s.getIdStatut())
                .code(s.getCode())
                .libelle(s.getLibelle())
                .build();
    }

    public static ReferenceDTO fromPriorite(Priorite p) {
        if (p == null) return null;
        return ReferenceDTO.builder()
                .id(p.getIdPriorite())
                .code(p.getCode())
                .libelle(p.getLibelle())
                .build();
    }

    public static ReferenceDTO fromService(mg.bank.backend.model.referentiel.Service s) {
        if (s == null) return null;
        return ReferenceDTO.builder()
                .id(s.getIdService())
                .libelle(s.getNom())
                .build();
    }

    public static ReferenceDTO fromTypeActivite(TypeActivite t) {
        if (t == null) return null;
        return ReferenceDTO.builder()
                .id(t.getIdTypeActivite())
                .libelle(t.getDesignation())
                .build();
    }

    public static ReferenceDTO fromSite(Site s) {
        if (s == null) return null;
        return ReferenceDTO.builder()
                .id(s.getIdSite())
                .libelle(s.getNom())
                .build();
    }

    public static ReferenceDTO fromObjectifSpecifique(ObjectifSpecifique o) {
        if (o == null) return null;
        return ReferenceDTO.builder()
                .id(o.getIdObjectifSpecifique())
                .code(o.getCode())
                .libelle(o.getDesignation())
                .annee(o.getAnnee())
                .build();
    }

    /* -------------------- depuis les projections -------------------- */

    /**
     * Reference courte depuis les champs separes d'une projection (row
     * native). Le libelle produit est "prenom nom" — les parametres sont
     * nommes dans l'ordre naturel d'appel pour eviter les inversions.
     */
    public static ReferenceDTO fromUtilisateur(Integer id, String nom, String prenom) {
        if (id == null) return null;
        return ReferenceDTO.builder()
                .id(id)
                .libelle(prenom + " " + nom)
                .build();
    }
}