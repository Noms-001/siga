-- =============================================================================
-- SAGA — Données de référence
--
-- À exécuter EN PREMIER, puis migration_code_generation.sql et trigger.sql.
-- Les données fictives de démonstration sont isolées dans demo_etats_analyse.sql.
-- Ce fichier ne contient que les référentiels et les comptes nécessaires à
-- l'application : aucune activité, sous-activité, valeur ou plan de test.
-- =============================================================================
BEGIN;
-- =============================================================================
-- 1. RÉFÉRENTIELS
-- =============================================================================
INSERT INTO departement (code, nom, description)
VALUES (
        'TSS',
        'Département Études, Sûreté et Sécurité',
        'Département chargé des études, de la sûreté et de la sécurité des biens et des personnes au sein de la BFM.'
    );
INSERT INTO service (
        nom,
        description,
        actif,
        date_desactivation,
        id_departement
    )
SELECT 'Gestion des Risques et Procédures',
    'Service chargé de l''identification, l''évaluation et la maîtrise des risques, ainsi que de l''élaboration et la diffusion des procédures de sûreté et de sécurité.',
    TRUE,
    NULL,
    d.id_departement
FROM departement d
WHERE d.code = 'TSS';
INSERT INTO service (
        nom,
        description,
        actif,
        date_desactivation,
        id_departement
    )
SELECT 'Gestion des Systèmes de Sécurité',
    'Service chargé de la disponibilité, la maintenance et l''amélioration des systèmes de sécurité (vidéosurveillance, alarme, contrôle d''accès, etc.).',
    TRUE,
    NULL,
    d.id_departement
FROM departement d
WHERE d.code = 'TSS';
INSERT INTO type_activite (
        designation,
        description,
        actif,
        date_creation,
        date_desactivation
    )
VALUES (
        'Étude',
        'Études techniques, audits, analyses de risques et diagnostics.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Installation',
        'Installation de nouveaux équipements ou systèmes de sécurité.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Maintenance préventive',
        'Maintenance planifiée et périodique des équipements.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Maintenance curative',
        'Interventions correctives suite à une panne ou défaillance.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Inspection',
        'Inspections, contrôles et vérifications sur site.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Procédure',
        'Élaboration, mise à jour et diffusion de procédures.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Exercice de sécurité',
        'Exercices, simulations et tests de sécurité.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Inventaire',
        'Inventaires et recensements des équipements.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Intervention urgente',
        'Interventions non planifiées suite à un incident.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    ),
    (
        'Amélioration',
        'Projets d''amélioration et de renforcement des dispositifs.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL
    );
INSERT INTO statut (
        code,
        libelle,
        description,
        actif,
        date_creation,
        date_modification,
        date_desactivation
    )
VALUES (
        'BROUILLON',
        'Brouillon',
        'Élément en cours de rédaction.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'EN_ATTENTE_VALIDATION',
        'En attente de validation',
        'Élément soumis.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'VALIDEE',
        'Validée',
        'Élément validé.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'REJETE',
        'Rejetée',
        'Élément rejeté.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'NON_COMMENCEE',
        'Non commencée',
        'Élément validé non démarré.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'EN_COURS',
        'En cours',
        'Élément en cours.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'TERMINEE',
        'Terminée',
        'Élément terminé.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'EN_RETARD',
        'En retard',
        'Élément en retard.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'SUSPENDUE',
        'Suspendue',
        'Élément suspendu.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'REPORTEE',
        'Reportée',
        'Élément reporté.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'ANNULEE',
        'Annulée',
        'Élément annulé.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    );
INSERT INTO priorite (
        code,
        description,
        actif,
        date_creation,
        date_desactivation,
        libelle
    )
VALUES (
        'FAIBLE',
        'Priorité faible.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        'Faible'
    ),
    (
        'NORMALE',
        'Priorité normale.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        'Normale'
    ),
    (
        'HAUTE',
        'Priorité haute.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        'Haute'
    ),
    (
        'CRITIQUE',
        'Priorité critique.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        'Critique'
    );
INSERT INTO role (
        code,
        designation,
        description,
        actif,
        date_creation,
        date_modification,
        date_desactivation
    )
VALUES (
        'RESPONSABLE',
        'Responsable',
        'Responsable principal.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'SUPPLEANT',
        'Suppléant',
        'Suppléant du responsable.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    ),
    (
        'PARTICIPANT',
        'Participant',
        'Participant.',
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL
    );
INSERT INTO type_token (code, libelle, description)
VALUES (
        'RESET_MOT_DE_PASSE',
        'Réinitialisation du mot de passe',
        'Token reset.'
    ),
    (
        'ACTIVATION_COMPTE',
        'Activation de compte',
        'Token activation.'
    ),
    (
        'AUTHENTIFICATION',
        'Authentification',
        'Token session.'
    );
INSERT INTO "procedure" (designation, description, actif)
VALUES (
        'Procédure de validation des activités PTA TSS',
        'Validation par le chef de service puis par le chef de département.',
        TRUE
    ),
    (
        'Procédure de validation des activités courantes TSS',
        'Validation par le chef de service puis par le chef de département.',
        TRUE
    ),
    (
        'Procédure de validation des plans d''action TSS',
        'Validation par le chef de service puis par le chef de département.',
        TRUE
    );
INSERT INTO etape_validation (
        designation,
        description,
        niveau,
        obligatoire,
        actif,
        date_desactivation,
        id_procedure,
        retour
    )
SELECT e.designation,
    e.description,
    e.niveau,
    TRUE,
    TRUE,
    NULL,
    p.id_procedure,
    e.retour
FROM "procedure" p
    CROSS JOIN (
        VALUES (
                'Technicien',
                'Vérification initiale de l''activité par le technicien.',
                1,
                FALSE
            ),
            (
                'Superviseur',
                'Vérification de l''activité par le superviseur.',
                2,
                FALSE
            ),
            (
                'Chef de service',
                'Vérification et validation de l''activité par le chef de service.',
                3,
                TRUE
            ),
            (
                'Chef de département',
                'Vérification et validation finale de l''activité par le chef de département.',
                4,
                TRUE
            )
    ) AS e(designation, description, niveau, retour)
WHERE p.designation IN (
        'Procédure de validation des activités PTA TSS',
        'Procédure de validation des activités courantes TSS',
        'Procédure de validation des plans d''action TSS'
    );
INSERT INTO poste (
        nom,
        effectif_prevu,
        effectif_reel,
        actif,
        date_creation,
        date_modification,
        date_desactivation,
        is_metier
    )
VALUES (
        'Chef de département TSS',
        1,
        1,
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL,
        TRUE
    ),
    (
        'Chef de service — Gestion des risques et Procédures',
        1,
        1,
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL,
        TRUE
    ),
    (
        'Gestionnaire risques',
        1,
        1,
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL,
        TRUE
    ),
    (
        'Chargé procédures',
        1,
        1,
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL,
        TRUE
    ),
    (
        'Chef de service — Gestion des systèmes de sécurité',
        1,
        1,
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL,
        TRUE
    ),
    (
        'Gestionnaire systèmes',
        1,
        1,
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL,
        TRUE
    ),
    (
        'Superviseur installation',
        2,
        2,
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL,
        TRUE
    ),
    (
        'Technicien systèmes',
        4,
        4,
        TRUE,
        '2020-01-15 08:00:00',
        NULL,
        NULL,
        TRUE
    ),
    (
        'Administrateur',
        1,
        1,
        TRUE,
        CURRENT_TIMESTAMP,
        NULL,
        NULL,
        FALSE
    );
INSERT INTO etape_validation_decideur (id_etape_validation, id_poste)
SELECT ev.id_etape_validation,
    p.id_poste
FROM etape_validation ev
    JOIN "procedure" pr ON pr.id_procedure = ev.id_procedure
    JOIN poste p ON (
        ev.designation = 'Technicien'
        AND p.nom = 'Technicien systèmes'
    )
    OR (
        ev.designation = 'Superviseur'
        AND p.nom = 'Superviseur installation'
    )
    OR (
        ev.designation = 'Chef de service'
        AND p.nom IN (
            'Chef de service — Gestion des risques et Procédures',
            'Chef de service — Gestion des systèmes de sécurité'
        )
    )
    OR (
        ev.designation = 'Chef de département'
        AND p.nom = 'Chef de département TSS'
    )
WHERE pr.designation IN (
        'Procédure de validation des activités PTA TSS',
        'Procédure de validation des activités courantes TSS',
        'Procédure de validation des plans d''action TSS'
    ) ON CONFLICT (id_etape_validation, id_poste) DO NOTHING;
-- =============================================================================
-- 2. PERMISSIONS
-- =============================================================================
-- Referentiel des permissions : une ligne par couple ressource/action.
-- Les actions et les ressources sont alignees sur les annotations
-- @RequirePermission des controllers.
-- =============================================================================
INSERT INTO permission (
        designation,
        description,
        ressource,
        action,
        actif,
        date_desactivation
    )
VALUES (
        'Consulter les activités',
        'Consulter les activités.',
        'activite',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les activités',
        'Créer les activités.',
        'activite',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les activités',
        'Modifier les activités.',
        'activite',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les activités',
        'Désactiver les activités.',
        'activite',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Valider les activités',
        'Valider les activités.',
        'activite',
        'VALIDER',
        TRUE,
        NULL
    ),
    (
        'Exporter les activités',
        'Exporter les activités.',
        'activite',
        'EXPORTER',
        TRUE,
        NULL
    ),
    (
        'Importer les activités',
        'Importer les activités.',
        'activite',
        'IMPORTER',
        TRUE,
        NULL
    ),
    (
        'Affecter les activités',
        'Affecter les activités.',
        'activite',
        'AFFECTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les sous-activités',
        'Consulter les sous-activités.',
        'sous_activite',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les sous-activités',
        'Créer les sous-activités.',
        'sous_activite',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les sous-activités',
        'Modifier les sous-activités.',
        'sous_activite',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les sous-activités',
        'Désactiver les sous-activités.',
        'sous_activite',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Importer les sous-activités',
        'Importer les sous-activités.',
        'sous_activite',
        'IMPORTER',
        TRUE,
        NULL
    ),
    (
        'Affecter les sous-activités',
        'Affecter les sous-activités.',
        'sous_activite',
        'AFFECTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les livrables',
        'Consulter les livrables.',
        'livrable',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les livrables',
        'Créer les livrables.',
        'livrable',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les livrables',
        'Modifier les livrables.',
        'livrable',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les livrables',
        'Désactiver les livrables.',
        'livrable',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les fichiers',
        'Consulter les fichiers.',
        'fichier',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les fichiers',
        'Créer les fichiers.',
        'fichier',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Consulter les avancements de sous-activité',
        'Consulter les avancements de sous-activité.',
        'avancement_sous_activite',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les avancements de sous-activité',
        'Créer les avancements de sous-activité.',
        'avancement_sous_activite',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les avancements de sous-activité',
        'Modifier les avancements de sous-activité.',
        'avancement_sous_activite',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Consulter les objectifs spécifiques',
        'Consulter les objectifs spécifiques.',
        'objectif_specifique',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les objectifs spécifiques',
        'Créer les objectifs spécifiques.',
        'objectif_specifique',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les objectifs spécifiques',
        'Modifier les objectifs spécifiques.',
        'objectif_specifique',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Consulter les validations d''activité',
        'Consulter les validations d''activité.',
        'validation_activite',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les validations de plan d''action',
        'Consulter les validations de plan d''action.',
        'validation_plan_action',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les affectations d''activité',
        'Consulter les affectations d''activité.',
        'affectation_activite',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les affectations de sous-activité',
        'Consulter les affectations de sous-activité.',
        'affectation_sous_activite',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les demandes de reprise',
        'Consulter les demandes de reprise.',
        'demande_reprise',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les demandes de reprise',
        'Créer les demandes de reprise.',
        'demande_reprise',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les demandes de reprise',
        'Modifier les demandes de reprise.',
        'demande_reprise',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Valider les demandes de reprise',
        'Valider les demandes de reprise.',
        'demande_reprise',
        'VALIDER',
        TRUE,
        NULL
    ),
    (
        'Consulter les demandes de reprise de plan',
        'Consulter les demandes de reprise de plan.',
        'demande_reprise_plan',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les demandes de reprise de plan',
        'Créer les demandes de reprise de plan.',
        'demande_reprise_plan',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les demandes de reprise de plan',
        'Modifier les demandes de reprise de plan.',
        'demande_reprise_plan',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Valider les demandes de reprise de plan',
        'Valider les demandes de reprise de plan.',
        'demande_reprise_plan',
        'VALIDER',
        TRUE,
        NULL
    ),
    (
        'Consulter les plans d''action',
        'Consulter les plans d''action.',
        'plan_action',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les plans d''action',
        'Créer les plans d''action.',
        'plan_action',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les plans d''action',
        'Modifier les plans d''action.',
        'plan_action',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les plans d''action',
        'Désactiver les plans d''action.',
        'plan_action',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Valider les plans d''action',
        'Valider les plans d''action.',
        'plan_action',
        'VALIDER',
        TRUE,
        NULL
    ),
    (
        'Exporter les plans d''action',
        'Exporter les plans d''action.',
        'plan_action',
        'EXPORTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les avancements de plan d''action',
        'Consulter les avancements de plan d''action.',
        'avancement_plan_action',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les avancements de plan d''action',
        'Créer les avancements de plan d''action.',
        'avancement_plan_action',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les avancements de plan d''action',
        'Modifier les avancements de plan d''action.',
        'avancement_plan_action',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Consulter les indicateurs',
        'Consulter les indicateurs.',
        'indicateur',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les indicateurs',
        'Créer les indicateurs.',
        'indicateur',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les indicateurs',
        'Modifier les indicateurs.',
        'indicateur',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les indicateurs',
        'Désactiver les indicateurs.',
        'indicateur',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les indicateurs',
        'Activer les indicateurs.',
        'indicateur',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Importer les indicateurs',
        'Importer les indicateurs.',
        'indicateur',
        'IMPORTER',
        TRUE,
        NULL
    ),
    (
        'Affecter les indicateurs',
        'Affecter les indicateurs.',
        'indicateur',
        'AFFECTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les valeurs d''indicateur',
        'Consulter les valeurs d''indicateur.',
        'valeur_indicateur',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les valeurs d''indicateur',
        'Créer les valeurs d''indicateur.',
        'valeur_indicateur',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Consulter les signalements',
        'Consulter les signalements.',
        'signalement',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les signalements',
        'Créer les signalements.',
        'signalement',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les signalements',
        'Modifier les signalements.',
        'signalement',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Consulter les notifications',
        'Consulter les notifications.',
        'notification',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter le tableau de bord',
        'Consulter le tableau de bord.',
        'dashboard',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter le suivi des membres',
        'Consulter le suivi des membres.',
        'membre_suivi',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les utilisateurs',
        'Consulter les utilisateurs.',
        'utilisateur',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les utilisateurs',
        'Créer les utilisateurs.',
        'utilisateur',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les utilisateurs',
        'Modifier les utilisateurs.',
        'utilisateur',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les utilisateurs',
        'Désactiver les utilisateurs.',
        'utilisateur',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les utilisateurs',
        'Activer les utilisateurs.',
        'utilisateur',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les départements',
        'Consulter les départements.',
        'departement',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les départements',
        'Créer les départements.',
        'departement',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les départements',
        'Modifier les départements.',
        'departement',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les départements',
        'Désactiver les départements.',
        'departement',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les départements',
        'Activer les départements.',
        'departement',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les services',
        'Consulter les services.',
        'service',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les services',
        'Créer les services.',
        'service',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les services',
        'Modifier les services.',
        'service',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les services',
        'Désactiver les services.',
        'service',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les services',
        'Activer les services.',
        'service',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les postes',
        'Consulter les postes.',
        'poste',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les postes',
        'Créer les postes.',
        'poste',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les postes',
        'Modifier les postes.',
        'poste',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les postes',
        'Désactiver les postes.',
        'poste',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les postes',
        'Activer les postes.',
        'poste',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les rôles',
        'Consulter les rôles.',
        'role',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les rôles',
        'Créer les rôles.',
        'role',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les rôles',
        'Modifier les rôles.',
        'role',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les rôles',
        'Désactiver les rôles.',
        'role',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les rôles',
        'Activer les rôles.',
        'role',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les permissions',
        'Consulter les permissions.',
        'permission',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les permissions de poste',
        'Consulter les permissions de poste.',
        'poste_permission',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Affecter les permissions de poste',
        'Affecter les permissions de poste.',
        'poste_permission',
        'AFFECTER',
        TRUE,
        NULL
    ),
    (
        'Modifier les permissions de poste',
        'Modifier les permissions de poste.',
        'poste_permission',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les permissions de poste',
        'Désactiver les permissions de poste.',
        'poste_permission',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les types d''activité',
        'Consulter les types d''activité.',
        'type_activite',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les types d''activité',
        'Créer les types d''activité.',
        'type_activite',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les types d''activité',
        'Modifier les types d''activité.',
        'type_activite',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les types d''activité',
        'Désactiver les types d''activité.',
        'type_activite',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les types d''activité',
        'Activer les types d''activité.',
        'type_activite',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les sites',
        'Consulter les sites.',
        'site',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les sites',
        'Créer les sites.',
        'site',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les sites',
        'Modifier les sites.',
        'site',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les sites',
        'Désactiver les sites.',
        'site',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les sites',
        'Activer les sites.',
        'site',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les priorités',
        'Consulter les priorités.',
        'priorite',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les statuts',
        'Consulter les statuts.',
        'statut',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Consulter les procédures',
        'Consulter les procédures.',
        'procedure',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les procédures',
        'Créer les procédures.',
        'procedure',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les procédures',
        'Modifier les procédures.',
        'procedure',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les procédures',
        'Désactiver les procédures.',
        'procedure',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les procédures',
        'Activer les procédures.',
        'procedure',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les paramètres',
        'Consulter les paramètres.',
        'parametre',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Modifier les paramètres',
        'Modifier les paramètres.',
        'parametre',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Désactiver les paramètres',
        'Désactiver les paramètres.',
        'parametre',
        'DESACTIVER',
        TRUE,
        NULL
    ),
    (
        'Activer les paramètres',
        'Activer les paramètres.',
        'parametre',
        'ACTIVER',
        TRUE,
        NULL
    ),
    (
        'Consulter les résultats intermédiaires',
        'Consulter les résultats intermédiaires.',
        'resultat_intermediaire',
        'CONSULTER',
        TRUE,
        NULL
    ),
    (
        'Créer les résultats intermédiaires',
        'Créer les résultats intermédiaires.',
        'resultat_intermediaire',
        'CREER',
        TRUE,
        NULL
    ),
    (
        'Modifier les résultats intermédiaires',
        'Modifier les résultats intermédiaires.',
        'resultat_intermediaire',
        'MODIFIER',
        TRUE,
        NULL
    ),
    (
        'Importer les résultats intermédiaires',
        'Importer les résultats intermédiaires.',
        'resultat_intermediaire',
        'IMPORTER',
        TRUE,
        NULL
    );
-- Les permissions sont des données de référence : elles conditionnent l'accès
-- aux écrans. Chaque poste reçoit l'ensemble des permissions actives, avec une
-- portée adaptée à son niveau hiérarchique. Ce bloc doit rester dans data.sql
-- et non dans le jeu de démonstration.
INSERT INTO poste_permission (id_poste, id_permission, portee)
SELECT p.id_poste,
    perm.id_permission,
    CASE
        WHEN p.nom = 'Administrateur' THEN 'TOUS'
        WHEN p.nom = 'Chef de département TSS' THEN 'DEPARTEMENT'
        WHEN p.nom LIKE 'Chef de service%' THEN 'SERVICE'
        ELSE 'UTILISATEUR'
    END::portee_permission
FROM poste p
    CROSS JOIN permission perm
WHERE perm.actif = TRUE ON CONFLICT (id_poste, id_permission) DO NOTHING;
-- =============================================================================
-- 3. UTILISATEURS
-- =============================================================================
INSERT INTO utilisateur (
        nom,
        prenom,
        email,
        telephone,
        mot_de_passe,
        actif,
        date_creation,
        date_desactivation,
        date_modification,
        date_derniere_connexion,
        id_departement,
        id_service,
        id_poste
    )
SELECT u.nom,
    u.prenom,
    u.email,
    u.telephone,
    '$2a$12$QOhcf.cJnnuhCFYxfb4Ob.RO6xY4BqW8pbVCgoYnX4K.YeHGZ8GGu',
    TRUE,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    NULL,
    d.id_departement,
    s.id_service,
    p.id_poste
FROM (
        VALUES (
                'RAKOTOARISOA',
                'Andry',
                'nomenachristian04@gmail.com',
                '+261 34 00 000 01',
                'Chef de département TSS',
                NULL
            ),
            (
                'RAVELOMANANTSOA',
                'Miora',
                'miora.ravelomanantsoa@example.mg',
                '+261 34 00 000 02',
                'Chef de service — Gestion des risques et Procédures',
                'Gestion des risques et Procédures'
            ),
            (
                'RANDRIANARISOA',
                'Hery',
                'hery.randrianarisoa@example.mg',
                '+261 34 00 000 03',
                'Gestionnaire risques',
                'Gestion des risques et Procédures'
            ),
            (
                'RAZAFINDRAKOTO',
                'Fara',
                'fara.razafindrakoto@example.mg',
                '+261 34 00 000 04',
                'Chargé procédures',
                'Gestion des risques et Procédures'
            ),
            (
                'RAKOTONDRABE',
                'Tiana',
                'myahaintso@gmail.com',
                '+261 34 00 000 05',
                'Chef de service — Gestion des systèmes de sécurité',
                'Gestion des systèmes de sécurité'
            ),
            (
                'ANDRIAMBOLOLONA',
                'Toky',
                'toky.andriambololona@example.mg',
                '+261 34 00 000 06',
                'Gestionnaire systèmes',
                'Gestion des systèmes de sécurité'
            ),
            (
                'RANDRIAMAMPIANINA',
                'Haja',
                'randrichris17@gmail.com',
                '+261 34 00 000 07',
                'Superviseur installation',
                'Gestion des systèmes de sécurité'
            ),
            (
                'RAZANADRAKOTO',
                'Feno',
                'feno.razanadrakoto@example.mg',
                '+261 34 00 000 08',
                'Superviseur installation',
                'Gestion des systèmes de sécurité'
            ),
            (
                'RAKOTOMALALA',
                'Nantenaina',
                'nantenaina.rakotomalala@example.mg',
                '+261 34 00 000 09',
                'Technicien systèmes',
                'Gestion des systèmes de sécurité'
            ),
            (
                'ANDRIANJAFY',
                'Solofo',
                'solofo.andrianjafy@example.mg',
                '+261 34 00 000 10',
                'Technicien systèmes',
                'Gestion des systèmes de sécurité'
            ),
            (
                'RAKOTONIRINA',
                'Voahirana',
                'voahirana.rakotonirina@example.mg',
                '+261 34 00 000 11',
                'Technicien systèmes',
                'Gestion des systèmes de sécurité'
            ),
            (
                'RANDRIANASOLO',
                'Mamy',
                'mamy.randrianasolo@example.mg',
                '+261 34 00 000 12',
                'Technicien systèmes',
                'Gestion des systèmes de sécurité'
            ),
            (
                'RANDRIANARISOA',
                'Christine',
                'christine.randrianarisoa@example.mg',
                '+261 34 00 000 13',
                'Administrateur',
                NULL
            )
    ) AS u(
        nom,
        prenom,
        email,
        telephone,
        poste_nom,
        service_nom
    )
    JOIN departement d ON d.code = 'TSS'
    JOIN poste p ON p.nom = u.poste_nom
    LEFT JOIN service s ON s.id_departement = d.id_departement
    AND LOWER(TRIM(s.nom)) = LOWER(TRIM(u.service_nom))
WHERE (
        u.service_nom IS NULL
        OR s.id_service IS NOT NULL
    )
    AND NOT EXISTS (
        SELECT 1
        FROM utilisateur e
        WHERE LOWER(e.email) = LOWER(u.email)
    );
INSERT INTO type_activite_service (id_service, id_type_activite)
SELECT s.id_service,
    t.id_type_activite
FROM service s,
    type_activite t
WHERE (
        s.nom = 'Gestion des Risques et Procédures'
        AND t.designation IN (
            'Étude',
            'Inspection',
            'Procédure',
            'Inventaire',
            'Amélioration'
        )
    )
    OR (
        s.nom = 'Gestion des Systèmes de Sécurité'
        AND t.designation IN (
            'Installation',
            'Maintenance préventive',
            'Maintenance curative',
            'Exercice de sécurité',
            'Intervention urgente',
            'Amélioration'
        )
    );
SELECT setval(
        pg_get_serial_sequence('departement', 'id_departement'),
        COALESCE(
            (
                SELECT MAX(id_departement)
                FROM departement
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('service', 'id_service'),
        COALESCE(
            (
                SELECT MAX(id_service)
                FROM service
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('type_activite', 'id_type_activite'),
        COALESCE(
            (
                SELECT MAX(id_type_activite)
                FROM type_activite
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('statut', 'id_statut'),
        COALESCE(
            (
                SELECT MAX(id_statut)
                FROM statut
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('priorite', 'id_priorite'),
        COALESCE(
            (
                SELECT MAX(id_priorite)
                FROM priorite
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('poste', 'id_poste'),
        COALESCE(
            (
                SELECT MAX(id_poste)
                FROM poste
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('utilisateur', 'id_utilisateur'),
        COALESCE(
            (
                SELECT MAX(id_utilisateur)
                FROM utilisateur
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('type_token', 'id_type_token'),
        COALESCE(
            (
                SELECT MAX(id_type_token)
                FROM type_token
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('role', 'id_role'),
        COALESCE(
            (
                SELECT MAX(id_role)
                FROM role
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('permission', 'id_permission'),
        COALESCE(
            (
                SELECT MAX(id_permission)
                FROM permission
            ),
            1
        )
    );
SELECT setval(
        pg_get_serial_sequence('"procedure"', 'id_procedure'),
        COALESCE(
            (
                SELECT MAX(id_procedure)
                FROM "procedure"
            ),
            1
        )
    );
-- =============================================================================
-- 4. PARAMÈTRES
-- =============================================================================
INSERT INTO parametre (
        code,
        designation,
        valeur,
        type_valeur,
        description,
        categorie,
        modifiable,
        actif,
        date_creation
    )
VALUES (
        'ID_PROCEDURE_PTA',
        'Procédure activités PTA',
        (
            SELECT id_procedure::text
            FROM "procedure"
            WHERE designation = 'Procédure de validation des activités PTA TSS'
        ),
        'INTEGER',
        'Procédure de validation PTA.',
        'VALIDATION',
        TRUE,
        TRUE,
        '2026-01-05 08:00:00'
    ),
    (
        'ID_PROCEDURE_NON_PTA',
        'Procédure activités NON PTA',
        (
            SELECT id_procedure::text
            FROM "procedure"
            WHERE designation = 'Procédure de validation des activités courantes TSS'
        ),
        'INTEGER',
        'Procédure de validation NON PTA.',
        'VALIDATION',
        TRUE,
        TRUE,
        '2026-01-05 08:00:00'
    ),
    (
        'DUREE_TOKEN_ACTIVATION_COMPTE',
        'Durée token activation',
        '1440',
        'INTEGER',
        'Durée du token d''activation.',
        'AUTHENTIFICATION',
        TRUE,
        TRUE,
        '2026-01-05 08:00:00'
    ),
    (
        'DUREE_TOKEN_RESET_MOT_DE_PASSE',
        'Durée token reset',
        '10',
        'INTEGER',
        'Durée du token de reset.',
        'AUTHENTIFICATION',
        TRUE,
        TRUE,
        '2026-01-05 08:00:00'
    ),
    (
        'NOTIFICATION_ACTIVITE_AVANT_ECHEANCE_JOURS',
        'Notif sous-activité J-2',
        '2',
        'INTEGER',
        'Nombre de jours avant échéance sous-activité.',
        'NOTIFICATION',
        TRUE,
        TRUE,
        '2026-01-05 08:00:00'
    ),
    (
        'NOTIFICATION_PLAN_ACTION_SEUIL_RETARD_HIERARCHIE_JOURS',
        'Seuil alerte hiérarchique plan',
        '7',
        'INTEGER',
        'Seuil de retard plan INCIDENT.',
        'NOTIFICATION',
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'NOTIFICATION_SOUS_ACTIVITE_AVANT_ECHEANCE_JOURS',
        'Notif sous-activité J-3',
        '3',
        'INTEGER',
        'Notification sous-activité.',
        'NOTIFICATION',
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'NOTIFICATION_SOUS_ACTIVITE_SEUIL_RETARD_HIERARCHIE_JOURS',
        'Seuil alerte SA',
        '7',
        'INTEGER',
        'Seuil alerte SA.',
        'NOTIFICATION',
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP
    ),
    (
        'NOTIFICATION_PLAN_ACTION_AVANT_ECHEANCE_JOURS',
        'Notif plan J-3',
        '3',
        'INTEGER',
        'Notification plan.',
        'NOTIFICATION',
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP
    );
-- 3. Paramètre des priorités urgentes
INSERT INTO parametre (
        code,
        designation,
        valeur,
        type_valeur,
        description,
        categorie,
        modifiable,
        actif,
        date_creation
    )
VALUES (
        'NOTIFICATION_EMAIL_PRIORITES',
        'Priorités déclenchant un email immédiat',
        'CRITIQUE,HAUTE',
        'STRING',
        'Liste des codes de priorité (séparés par virgule) pour lesquels un email ' || 'est envoyé automatiquement. Modifiable sans redéploiement.',
        'NOTIFICATION',
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP
    ) ON CONFLICT (code) DO NOTHING;
-- =============================================================================
-- 5. INITIALISATION DES COMPTEURS D'ACTIVITÉ
-- =============================================================================
-- Les fixtures de démonstration sont isolées dans demo_etats_analyse.sql.
INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'ACT:PTA:' || a.id_service || ':' || code_parts [1] || ':' || a.id_objectif_specifique,
    MAX(code_parts [3]::INTEGER)
FROM activite a
    JOIN objectif_specifique os ON os.id_objectif_specifique = a.id_objectif_specifique
    CROSS JOIN LATERAL regexp_match(a.code, '^A-([0-9]{4})-([0-9]+)-([0-9]+)$') code_parts
    CROSS JOIN LATERAL regexp_match(os.code, '([0-9]+)$') objectif_parts
WHERE code_parts IS NOT NULL
    AND objectif_parts IS NOT NULL
    AND code_parts [2]::INTEGER = objectif_parts [1]::INTEGER
GROUP BY code_parts [1],
    a.id_service,
    a.id_objectif_specifique ON CONFLICT (cle) DO
UPDATE
SET dernier_numero = GREATEST(
        code_sequence.dernier_numero,
        EXCLUDED.dernier_numero
    );
INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'ACT:NON_PTA:' || a.id_service || ':' || code_parts [1],
    MAX(code_parts [2]::INTEGER)
FROM activite a
    CROSS JOIN LATERAL regexp_match(a.code, '^A-([0-9]{4})-([0-9]+)$') code_parts
WHERE a.id_objectif_specifique IS NULL
    AND code_parts IS NOT NULL
GROUP BY a.id_service,
    code_parts [1] ON CONFLICT (cle) DO
UPDATE
SET dernier_numero = GREATEST(
        code_sequence.dernier_numero,
        EXCLUDED.dernier_numero
    );
INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'SA:' || parent.id_activite,
    MAX(
        substring(
            sa.code
            FROM char_length(
                    'SA-' || substring(
                        parent.code
                        FROM 3
                    ) || '-'
                ) + 1
        )::INTEGER
    )
FROM sous_activite sa
    JOIN activite parent ON parent.id_activite = sa.id_activite
WHERE left(
        sa.code,
        char_length(
            'SA-' || substring(
                parent.code
                FROM 3
            ) || '-'
        )
    ) = 'SA-' || substring(
        parent.code
        FROM 3
    ) || '-'
    AND substring(
        sa.code
        FROM char_length(
                'SA-' || substring(
                    parent.code
                    FROM 3
                ) || '-'
            ) + 1
    ) ~ '^[0-9]+$'
GROUP BY parent.id_activite ON CONFLICT (cle) DO
UPDATE
SET dernier_numero = GREATEST(
        code_sequence.dernier_numero,
        EXCLUDED.dernier_numero
    );
COMMIT;