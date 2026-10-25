package mg.bank.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


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

    /**
     * Filtre à transmettre aux requêtes SQL : le service du périmètre
     * s'il est rattaché, sinon null (pas de filtre).
     */
    public Integer getFiltreService() {
        return estRattacheService() ? idService : null;
    }

    /**
     * Filtre à transmettre aux requêtes SQL : le département du périmètre
     * s'il est au niveau département, sinon null (pas de filtre).
     */
    public Integer getFiltreDepartement() {
        return estNiveauDepartement() ? idDepartement : null;
    }

}
