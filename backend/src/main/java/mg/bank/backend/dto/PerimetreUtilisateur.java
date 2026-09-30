package mg.bank.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Perimetre impose par le backend a partir de l'utilisateur connecte.
 *
 * Cette classe n'est jamais remplie depuis une requete HTTP : elle est
 * construite par le service a partir de l'Utilisateur authentifie. C'est
 * ce qui garantit qu'un utilisateur ne peut pas elargir son perimetre en
 * forgeant un parametre.
 */
@Getter
@Builder
@AllArgsConstructor
public class PerimetreUtilisateur {

    private Integer idService;

    private Integer idDepartement;

    /**
     * Niveau departement : le filtre Service est propose, liste et
     * statistiques restreintes au departement.
     */
    public boolean estNiveauDepartement() {
        return idService == null && idDepartement != null;
    }

    /**
     * Rattache a un service : le filtre Service n'est pas propose et le
     * perimetre est bloque sur son service.
     */
    public boolean estRattacheService() {
        return idService != null && idDepartement != null;
    }

    /**
     * Aucun rattachement : aucun filtre de perimetre n'est applique.
     */
    public boolean estSansPerimetre() {
        return idService == null && idDepartement == null;
    }

}
