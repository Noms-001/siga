package mg.bank.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.enums.PorteePermission;
import mg.bank.backend.security.PorteeAcces;

/**
 * Perimetre de l'utilisateur connecte.
 *
 * Deux sources sont possibles :
 *
 * <ul>
 *   <li>le rattachement organisationnel de l'utilisateur (service,
 *       departement) utilise par defaut, notamment hors requete protegee ;</li>
 *   <li>la portee de la permission qui a autorise la requete, lorsqu'elle est
 *       disponible. Elle prime alors sur le rattachement : un utilisateur dont
 *       la permission est TOUS voir tout, meme s'il est attache a un service.</li>
 * </ul>
 */
@Getter
@Builder
@AllArgsConstructor
public class PerimetreUtilisateur {

    private Integer idService;

    private Integer idDepartement;

    private PorteeAcces porteeAcces;

    /** Niveau departement : service propose, liste et statistiques limitees au departement. */
    public boolean estNiveauDepartement() {
        if (porteeAcces != null) {
            return porteeAcces.estDepartement();
        }
        return idService == null && idDepartement != null;
    }

    /** Rattache a un service : le perimetre est bloque sur son service. */
    public boolean estRattacheService() {
        if (porteeAcces != null) {
            return porteeAcces.estService();
        }
        return idService != null;
    }

    /**
     * Aucun filtre organisationnel. Une portee UTILISATEUR n'est pas « sans
     * perimetre » : elle est meme plus stricte qu'un service.
     */
    public boolean estSansPerimetre() {
        if (porteeAcces != null) {
            return porteeAcces.estTous();
        }
        return idService == null && idDepartement == null;
    }

    /** Filtre a transmettre aux requetes SQL pour le service. */
    public Integer getFiltreService() {
        if (porteeAcces != null) {
            return porteeAcces.filtreService();
        }
        return estRattacheService() ? idService : null;
    }

    /** Filtre a transmettre aux requetes SQL pour le departement. */
    public Integer getFiltreDepartement() {
        if (porteeAcces != null) {
            return porteeAcces.filtreDepartement();
        }
        return estNiveauDepartement() ? idDepartement : null;
    }

    /**
     * Filtre a transmettre aux requetes SQL pour une portee UTILISATEUR :
     * auteur (historique BROUILLON) ou affecte actif.
     */
    public Integer getIdUtilisateurPortee() {
        return porteeAcces == null ? null : porteeAcces.filtreUtilisateur();
    }

    /** Portee effective, ou null hors controle de permission. */
    public PorteePermission getPortee() {
        return porteeAcces == null ? null : porteeAcces.portee();
    }

    /** Vrai lorsque la portee autorise l'acces a toutes les donnees. */
    public boolean estPorteeTous() {
        return porteeAcces != null && porteeAcces.estTous();
    }
}
