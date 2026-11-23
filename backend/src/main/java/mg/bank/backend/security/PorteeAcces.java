package mg.bank.backend.security;

import java.util.Comparator;

import mg.bank.backend.enums.PorteePermission;

/**
 * Portee effective d'une permission pour l'utilisateur connecte.
 *
 * La portee est une propriete de l'affectation poste/permission. Pour un
 * endpoint qui declare plusieurs permissions alternatives (heritages parent /
 * enfant), c'est la portee la plus large qui est retenue : avoir acces au
 * parent donne acces a l'enfant.
 *
 * Les valeurs retournees par les filtres sont directement exploitables par les
 * requetes SQL. {@link #SANS_ACCES} est un identifiant impossible qui force un
 * resultat vide lorsqu'une portee hierarchique est demandee a un utilisateur
 * sans rattachement.
 */
public record PorteeAcces(
        PorteePermission portee,
        Integer idUtilisateur,
        Integer idService,
        Integer idDepartement) {

    public static final int SANS_ACCES = -1;

    private static final Comparator<PorteePermission> LARGEUR = Comparator.comparingInt(PorteeAcces::rang);

    public PorteeAcces {
        if (portee == null) {
            portee = PorteePermission.UTILISATEUR;
        }
    }

    private static int rang(PorteePermission portee) {
        return switch (portee) {
            case UTILISATEUR -> 0;
            case SERVICE -> 1;
            case DEPARTEMENT -> 2;
            case TOUS -> 3;
        };
    }

    /** La plus large des portees, null etant traite comme UTILISATEUR. */
    public static PorteePermission laPlusLarge(Iterable<PorteePermission> portees) {
        PorteePermission retenue = null;
        for (PorteePermission portee : portees) {
            PorteePermission effective = portee == null ? PorteePermission.UTILISATEUR : portee;
            if (retenue == null || LARGEUR.compare(effective, retenue) > 0) {
                retenue = effective;
            }
        }
        return retenue;
    }

    /** Construit la portee effective d'une requete protegee. */
    public static PorteeAcces de(
            PorteePermission portee,
            Integer idUtilisateur,
            Integer idService,
            Integer idDepartement) {
        return new PorteeAcces(portee, idUtilisateur, idService, idDepartement);
    }

    public boolean estUtilisateur() {
        return portee == PorteePermission.UTILISATEUR;
    }

    public boolean estService() {
        return portee == PorteePermission.SERVICE;
    }

    public boolean estDepartement() {
        return portee == PorteePermission.DEPARTEMENT;
    }

    public boolean estTous() {
        return portee == PorteePermission.TOUS;
    }

    /** Filtre SQL sur le service : renseigne uniquement pour une portee SERVICE. */
    public Integer filtreService() {
        if (!estService()) {
            return null;
        }
        return idService == null ? SANS_ACCES : idService;
    }

    /** Filtre SQL sur le departement : renseigne uniquement pour une portee DEPARTEMENT. */
    public Integer filtreDepartement() {
        if (!estDepartement()) {
            return null;
        }
        return idDepartement == null ? SANS_ACCES : idDepartement;
    }

    /**
     * Filtre SQL sur l'auteur / l'affecte : renseigne uniquement pour une
     * portee UTILISATEUR.
     */
    public Integer filtreUtilisateur() {
        if (!estUtilisateur()) {
            return null;
        }
        return idUtilisateur == null ? SANS_ACCES : idUtilisateur;
    }
}
