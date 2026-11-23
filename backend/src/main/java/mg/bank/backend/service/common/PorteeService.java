package mg.bank.backend.service.common;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.enums.PorteePermission;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.indicateur.IndicateurRepository;
import mg.bank.backend.repository.planaction.PlanActionRepository;
import mg.bank.backend.security.PorteeAcces;
import mg.bank.backend.service.utilisateur.CurrentUserService;

/**
 * Point unique d'application de la portee des permissions.
 *
 * <p>Les methodes sans {@link PerimetreUtilisateur} en parametre resolvent le
 * perimetre de la requete courante. Les variantes avec perimetre permettent de
 * resoudre le perimetre une seule fois dans une boucle (listes filtrees) et
 * evitent ainsi une requete utilisateur par ligne.</p>
 */
@Service
@RequiredArgsConstructor
public class PorteeService {

    private final CurrentUserService currentUserService;
    private final ActiviteRepository activiteRepository;
    private final PlanActionRepository planActionRepository;
    private final IndicateurRepository indicateurRepository;

    public PerimetreUtilisateur perimetreCourant() {
        return currentUserService.getPerimetre();
    }

    /*
     * L'ordre de PorteePermission va du plus restrictif au plus large :
     * UTILISATEUR < SERVICE < DEPARTEMENT < TOUS.
     */
    public boolean porteeAuMoins(PorteePermission minimale) {
        return porteeEffective(perimetreCourant()).compareTo(minimale) >= 0;
    }

    public boolean porteeTous() {
        return porteeAuMoins(PorteePermission.TOUS);
    }

    public boolean porteeTous(PerimetreUtilisateur perimetre) {
        return porteeEffective(perimetre) == PorteePermission.TOUS;
    }

    public boolean porteeServiceOuPlus() {
        return porteeAuMoins(PorteePermission.SERVICE);
    }

    public boolean porteeDepartementOuPlus() {
        return porteeAuMoins(PorteePermission.DEPARTEMENT);
    }

    public boolean departementAccessible(Integer idDepartement) {
        return departementAccessible(idDepartement, perimetreCourant());
    }

    public boolean departementAccessible(
            Integer idDepartement,
            PerimetreUtilisateur perimetre) {

        if (idDepartement == null) {
            return false;
        }
        if (porteeEffective(perimetre) == PorteePermission.TOUS) {
            return true;
        }
        return perimetre.getIdDepartement() != null
                && perimetre.getIdDepartement().equals(idDepartement);
    }

    public boolean serviceAccessible(
            mg.bank.backend.model.referentiel.Service service) {
        return serviceAccessible(service, perimetreCourant());
    }

    public boolean serviceAccessible(
            mg.bank.backend.model.referentiel.Service service,
            PerimetreUtilisateur perimetre) {

        if (service == null || service.getIdService() == null) {
            return false;
        }
        PorteePermission portee = porteeEffective(perimetre);
        if (portee == PorteePermission.TOUS) {
            return true;
        }
        if (portee == PorteePermission.DEPARTEMENT) {
            return service.getDepartement() != null
                    && perimetre.getIdDepartement() != null
                    && perimetre.getIdDepartement().equals(
                            service.getDepartement().getIdDepartement());
        }
        return perimetre.getIdService() != null
                && perimetre.getIdService().equals(service.getIdService());
    }

    public void exigerDepartement(Integer idDepartement) {
        if (!departementAccessible(idDepartement)) {
            throw new ApiException("Département introuvable", HttpStatus.NOT_FOUND);
        }
    }

    public void exigerService(mg.bank.backend.model.referentiel.Service service) {
        if (!serviceAccessible(service)) {
            throw new ApiException("Service introuvable", HttpStatus.NOT_FOUND);
        }
    }

    public Integer filtreDepartement() {
        PerimetreUtilisateur perimetre = perimetreCourant();
        if (porteeEffective(perimetre) != PorteePermission.DEPARTEMENT) {
            return null;
        }
        return perimetre.getIdDepartement() == null
                ? PorteeAcces.SANS_ACCES
                : perimetre.getIdDepartement();
    }

    public Integer filtreService() {
        PerimetreUtilisateur perimetre = perimetreCourant();
        if (porteeEffective(perimetre) != PorteePermission.SERVICE) {
            return null;
        }
        return perimetre.getIdService() == null
                ? PorteeAcces.SANS_ACCES
                : perimetre.getIdService();
    }

    public Integer filtreUtilisateur() {
        PerimetreUtilisateur perimetre = perimetreCourant();
        if (porteeEffective(perimetre) != PorteePermission.UTILISATEUR) {
            return null;
        }
        return perimetre.getIdUtilisateurPortee() == null
                ? PorteeAcces.SANS_ACCES
                : perimetre.getIdUtilisateurPortee();
    }

    /**
     * Combine un filtre demande par le client avec le filtre autorise par la
     * portee. Un identifiant different de celui autorise est remplace par
     * {@link PorteeAcces#SANS_ACCES} afin de produire une liste vide.
     */
    public Integer restreindreFiltre(Integer demande, Integer autorise) {
        if (autorise == null) {
            return demande;
        }
        if (demande == null) {
            return autorise;
        }
        return autorise.equals(demande) ? demande : PorteeAcces.SANS_ACCES;
    }

    public Integer restreindreDepartement(Integer idDepartement) {
        return restreindreFiltre(idDepartement, filtreDepartement());
    }

    public Integer restreindreService(Integer idService) {
        return restreindreFiltre(idService, filtreService());
    }

    public boolean activiteAccessible(Integer idActivite) {
        return activiteAccessible(idActivite, perimetreCourant());
    }

    public boolean activiteAccessible(
            Integer idActivite,
            PerimetreUtilisateur perimetre) {

        if (idActivite == null || perimetre == null) {
            return false;
        }
        return activiteRepository.findActiviteDetail(
                idActivite,
                perimetre.getFiltreService(),
                perimetre.getFiltreDepartement(),
                perimetre.getIdUtilisateurPortee(),
                null).isPresent();
    }

    public void exigerActivite(Integer idActivite) {
        if (!activiteAccessible(idActivite)) {
            throw new ApiException("Activité introuvable", HttpStatus.NOT_FOUND);
        }
    }

    public boolean planActionAccessible(Integer idPlanAction) {
        return planActionAccessible(idPlanAction, perimetreCourant());
    }

    public boolean planActionAccessible(
            Integer idPlanAction,
            PerimetreUtilisateur perimetre) {

        if (idPlanAction == null || perimetre == null) {
            return false;
        }
        return planActionRepository.findByIdWithDetails(
                idPlanAction,
                perimetre.getFiltreService(),
                perimetre.getFiltreDepartement(),
                perimetre.getIdUtilisateurPortee()).isPresent();
    }

    public void exigerPlanAction(Integer idPlanAction) {
        if (!planActionAccessible(idPlanAction)) {
            throw new ApiException("Plan d'action introuvable.", HttpStatus.NOT_FOUND);
        }
    }

    public boolean utilisateurAccessible(Utilisateur utilisateur) {
        return utilisateurAccessible(utilisateur, perimetreCourant());
    }

    public boolean utilisateurAccessible(
            Utilisateur utilisateur,
            PerimetreUtilisateur perimetre) {

        if (utilisateur == null) {
            return false;
        }
        PorteePermission portee = porteeEffective(perimetre);
        if (portee == PorteePermission.TOUS) {
            return true;
        }
        if (portee == PorteePermission.UTILISATEUR) {
            return perimetre.getIdUtilisateurPortee() != null
                    && perimetre.getIdUtilisateurPortee()
                            .equals(utilisateur.getIdUtilisateur());
        }
        Integer idService = utilisateur.getService() == null
                ? null
                : utilisateur.getService().getIdService();
        Integer idDepartement = utilisateur.getDepartement() == null
                ? null
                : utilisateur.getDepartement().getIdDepartement();
        if (portee == PorteePermission.SERVICE) {
            return perimetre.getFiltreService() != null
                    && perimetre.getFiltreService().equals(idService);
        }
        return perimetre.getFiltreDepartement() != null
                && perimetre.getFiltreDepartement().equals(idDepartement);
    }

    public void exigerUtilisateur(Utilisateur utilisateur) {
        if (!utilisateurAccessible(utilisateur)) {
            throw new ApiException("Utilisateur introuvable", HttpStatus.NOT_FOUND);
        }
    }

    public boolean indicateurAccessible(Integer idIndicateur) {
        return indicateurAccessible(idIndicateur, perimetreCourant());
    }

    public boolean indicateurAccessible(
            Integer idIndicateur,
            PerimetreUtilisateur perimetre) {

        if (idIndicateur == null) {
            return false;
        }
        if (perimetre == null || porteeEffective(perimetre) == PorteePermission.TOUS) {
            return true;
        }
        return indicateurRepository.compterDansPerimetre(
                idIndicateur,
                perimetre.getFiltreService(),
                perimetre.getFiltreDepartement(),
                perimetre.getIdUtilisateurPortee()) > 0;
    }

    public void exigerIndicateur(Integer idIndicateur) {
        if (!indicateurAccessible(idIndicateur)) {
            throw new ApiException("Indicateur introuvable", HttpStatus.NOT_FOUND);
        }
    }

    private static PorteePermission porteeEffective(PerimetreUtilisateur perimetre) {
        if (perimetre == null) {
            return PorteePermission.TOUS;
        }
        if (perimetre.getPortee() != null) {
            return perimetre.getPortee();
        }
        if (perimetre.estRattacheService()) {
            return PorteePermission.SERVICE;
        }
        if (perimetre.estNiveauDepartement()) {
            return PorteePermission.DEPARTEMENT;
        }
        return PorteePermission.TOUS;
    }
}
