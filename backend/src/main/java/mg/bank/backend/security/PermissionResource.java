package mg.bank.backend.security;

/**
 * Ressources protegees par le systeme de permissions.
 *
 * La valeur est comparee sans tenir compte de la casse a la colonne
 * permission.ressource, stockee en minuscules dans data.sql.
 */
public final class PermissionResource {

    public static final String ACTIVITE = "ACTIVITE";
    public static final String SOUS_ACTIVITE = "SOUS_ACTIVITE";
    public static final String LIVRABLE = "LIVRABLE";
    public static final String FICHIER = "FICHIER";
    public static final String AVANCEMENT_SOUS_ACTIVITE = "AVANCEMENT_SOUS_ACTIVITE";
    public static final String OBJECTIF_SPECIFIQUE = "OBJECTIF_SPECIFIQUE";
    public static final String VALIDATION_ACTIVITE = "VALIDATION_ACTIVITE";
    public static final String VALIDATION_PLAN_ACTION = "VALIDATION_PLAN_ACTION";
    public static final String AFFECTATION_ACTIVITE = "AFFECTATION_ACTIVITE";
    public static final String AFFECTATION_SOUS_ACTIVITE = "AFFECTATION_SOUS_ACTIVITE";
    public static final String DEMANDE_REPRISE = "DEMANDE_REPRISE";
    public static final String DEMANDE_REPRISE_PLAN = "DEMANDE_REPRISE_PLAN";
    public static final String PLAN_ACTION = "PLAN_ACTION";
    public static final String AVANCEMENT_PLAN_ACTION = "AVANCEMENT_PLAN_ACTION";
    public static final String INDICATEUR = "INDICATEUR";
    public static final String VALEUR_INDICATEUR = "VALEUR_INDICATEUR";
    public static final String SIGNALEMENT = "SIGNALEMENT";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String DASHBOARD = "DASHBOARD";
    public static final String MEMBRE_SUIVI = "MEMBRE_SUIVI";
    public static final String UTILISATEUR = "UTILISATEUR";
    public static final String DEPARTEMENT = "DEPARTEMENT";
    public static final String SERVICE = "SERVICE";
    public static final String POSTE = "POSTE";
    public static final String ROLE = "ROLE";
    public static final String PERMISSION = "PERMISSION";
    public static final String POSTE_PERMISSION = "POSTE_PERMISSION";
    public static final String TYPE_ACTIVITE = "TYPE_ACTIVITE";
    public static final String SITE = "SITE";
    public static final String PRIORITE = "PRIORITE";
    public static final String STATUT = "STATUT";
    public static final String PROCEDURE = "PROCEDURE";
    public static final String PARAMETRE = "PARAMETRE";
    public static final String RESULTAT_INTERMEDIAIRE = "RESULTAT_INTERMEDIAIRE";

    private PermissionResource() {
    }
}
