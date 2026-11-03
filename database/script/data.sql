-- =============================================================================
-- SAGA — Données de référence + données de test
--
-- À exécuter EN PREMIER, puis triggers.sql.
-- Le trigger d'indicateur et les 3 fonctions cron sont dans triggers.sql.
-- Les notifications du trigger se déclencheront au bloc de démonstration
-- de triggers.sql, pas ici.
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
-- =============================================================================
-- 2. PERMISSIONS
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
        'Consulter une activité',
        'Consulter les activités.',
        'activite',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer une activité',
        'Créer une nouvelle activité.',
        'activite',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier une activité',
        'Modifier un brouillon.',
        'activite',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Supprimer une activité',
        'Supprimer un brouillon.',
        'activite',
        'DELETE',
        TRUE,
        NULL
    ),
    (
        'Démarrer une activité',
        'Démarrer une activité validée.',
        'activite',
        'DEMARRER',
        TRUE,
        NULL
    ),
    (
        'Clôturer une activité',
        'Clôturer une activité en cours.',
        'activite',
        'CLOTURER',
        TRUE,
        NULL
    ),
    (
        'Lire les validations d''activité',
        'Consulter les activités à valider.',
        'validation_activite',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Valider une activité',
        'Valider, rejeter ou renvoyer.',
        'validation_activite',
        'VALIDATE',
        TRUE,
        NULL
    ),
    (
        'Lire les sous-activités',
        'Consulter les sous-activités.',
        'sous_activite',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer une sous-activité',
        'Créer une sous-activité.',
        'sous_activite',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier une sous-activité',
        'Modifier une sous-activité.',
        'sous_activite',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Lire les avancements',
        'Consulter les avancements.',
        'avancement_sous_activite',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Saisir un avancement de sous-activité',
        'Saisir un avancement.',
        'avancement_sous_activite',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Lire les livrables',
        'Consulter les livrables.',
        'livrable',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un livrable',
        'Créer un livrable.',
        'livrable',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Supprimer un livrable',
        'Supprimer un livrable.',
        'livrable',
        'DELETE',
        TRUE,
        NULL
    ),
    (
        'Lire les objectifs spécifiques',
        'Consulter les objectifs.',
        'objectif_specifique',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un objectif spécifique',
        'Créer un objectif.',
        'objectif_specifique',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier un objectif spécifique',
        'Modifier un objectif.',
        'objectif_specifique',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Supprimer un objectif spécifique',
        'Supprimer un objectif.',
        'objectif_specifique',
        'DELETE',
        TRUE,
        NULL
    ),
    (
        'Lire les affectations d''activité',
        'Consulter les affectations.',
        'affectation_activite',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer une affectation d''activité',
        'Affecter une activité.',
        'affectation_activite',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Supprimer une affectation d''activité',
        'Retirer une affectation.',
        'affectation_activite',
        'DELETE',
        TRUE,
        NULL
    ),
    (
        'Lire les affectations de sous-activité',
        'Consulter les affectations SA.',
        'affectation_sous_activite',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer une affectation de sous-activité',
        'Affecter une sous-activité.',
        'affectation_sous_activite',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Supprimer une affectation de sous-activité',
        'Retirer une affectation SA.',
        'affectation_sous_activite',
        'DELETE',
        TRUE,
        NULL
    ),
    (
        'Lire les demandes de reprise',
        'Consulter les demandes.',
        'demande_reprise',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer une demande de reprise',
        'Demander une reprise.',
        'demande_reprise',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Traiter une demande de reprise',
        'Traiter une demande.',
        'demande_reprise',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Lire les demandes de reprise de plan',
        'Consulter les demandes plan.',
        'demande_reprise_plan',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer une demande de reprise de plan',
        'Demander une reprise plan.',
        'demande_reprise_plan',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Traiter une demande de reprise de plan',
        'Traiter une demande plan.',
        'demande_reprise_plan',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Lire les plans d''action',
        'Consulter les plans.',
        'plan_action',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un plan d''action',
        'Créer un plan.',
        'plan_action',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier un plan d''action',
        'Modifier un plan.',
        'plan_action',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les plans d''action',
        'Gérer les plans.',
        'plan_action',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Lire les avancements de plan',
        'Consulter les avancements.',
        'avancement_plan_action',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Saisir un avancement de plan',
        'Saisir un avancement.',
        'avancement_plan_action',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Consulter les indicateurs',
        'Consulter les indicateurs.',
        'indicateur',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Gérer les indicateurs',
        'Gérer les indicateurs.',
        'indicateur',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les valeurs d''indicateur',
        'Consulter les valeurs.',
        'valeur_indicateur',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Saisir une valeur d''indicateur',
        'Saisir une valeur.',
        'valeur_indicateur',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Consulter les signalements',
        'Consulter les signalements.',
        'signalement',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un signalement',
        'Créer un signalement.',
        'signalement',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Traiter un signalement',
        'Traiter un signalement.',
        'signalement',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Consulter les notifications',
        'Consulter les notifications.',
        'notification',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Modifier les notifications',
        'Marquer comme lues.',
        'notification',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Consulter le tableau de bord',
        'Consulter le dashboard.',
        'dashboard',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Consulter les utilisateurs',
        'Consulter les utilisateurs.',
        'utilisateur',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un utilisateur',
        'Créer un utilisateur.',
        'utilisateur',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les utilisateurs',
        'Modifier les utilisateurs.',
        'utilisateur',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les utilisateurs',
        'Gérer les utilisateurs.',
        'utilisateur',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les départements',
        'Consulter les départements.',
        'departement',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un département',
        'Créer un département.',
        'departement',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les départements',
        'Modifier un département.',
        'departement',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les départements',
        'Gérer les départements.',
        'departement',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les services',
        'Consulter les services.',
        'service',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un service',
        'Créer un service.',
        'service',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les services',
        'Modifier un service.',
        'service',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les services',
        'Gérer les services.',
        'service',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les postes',
        'Consulter les postes.',
        'poste',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un poste',
        'Créer un poste.',
        'poste',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les postes',
        'Modifier un poste.',
        'poste',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les postes',
        'Gérer les postes.',
        'poste',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les rôles',
        'Consulter les rôles.',
        'role',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un rôle',
        'Créer un rôle.',
        'role',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les rôles',
        'Modifier un rôle.',
        'role',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les rôles',
        'Gérer les rôles.',
        'role',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les permissions',
        'Consulter les permissions.',
        'permission',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Consulter les permissions de poste',
        'Consulter permissions poste.',
        'poste_permission',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Gérer les permissions de poste',
        'Gérer permissions poste.',
        'poste_permission',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les types d''activité',
        'Consulter les types.',
        'type_activite',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un type d''activité',
        'Créer un type.',
        'type_activite',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les types d''activité',
        'Modifier un type.',
        'type_activite',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les types d''activité',
        'Gérer les types.',
        'type_activite',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les sites',
        'Consulter les sites.',
        'site',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer un site',
        'Créer un site.',
        'site',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les sites',
        'Modifier un site.',
        'site',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les sites',
        'Gérer les sites.',
        'site',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les priorités',
        'Consulter les priorités.',
        'priorite',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer une priorité',
        'Créer une priorité.',
        'priorite',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les priorités',
        'Modifier une priorité.',
        'priorite',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les priorités',
        'Gérer les priorités.',
        'priorite',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les statuts',
        'Consulter les statuts.',
        'statut',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Consulter les procédures',
        'Consulter les procédures.',
        'procedure',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Créer une procédure',
        'Créer une procédure.',
        'procedure',
        'CREATE',
        TRUE,
        NULL
    ),
    (
        'Modifier les procédures',
        'Modifier une procédure.',
        'procedure',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les procédures',
        'Gérer les procédures.',
        'procedure',
        'MANAGE',
        TRUE,
        NULL
    ),
    (
        'Consulter les paramètres',
        'Consulter les paramètres.',
        'parametre',
        'READ',
        TRUE,
        NULL
    ),
    (
        'Modifier les paramètres',
        'Modifier les paramètres.',
        'parametre',
        'UPDATE',
        TRUE,
        NULL
    ),
    (
        'Gérer les paramètres',
        'Gérer les paramètres.',
        'parametre',
        'MANAGE',
        TRUE,
        NULL
    );
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
                'andry.rakotoarisoa@example.mg',
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
                'tiana.rakotondrabe@example.mg',
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
                'haja.randriamampianina@example.mg',
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
                'RANDRICH',
                'Chris',
                'randrichris17@gmail.com',
                NULL,
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
UPDATE utilisateur u
SET id_poste = p.id_poste,
    id_departement = d.id_departement,
    id_service = NULL,
    actif = TRUE,
    date_desactivation = NULL
FROM poste p
    CROSS JOIN departement d
WHERE lower(u.email) = lower('randrichris17@gmail.com')
    AND p.nom = 'Administrateur'
    AND d.code = 'TSS';
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
-- 5. DONNÉES DE TEST
-- =============================================================================
-- Origine INCIDENT (pour alerte hiérarchique)
INSERT INTO origine (
        code,
        designation,
        type_origine,
        description,
        id_utilisateur
    )
SELECT 'TORI-INC-01',
    'Incident de test',
    'INCIDENT',
    'Origine utilisée pour tester l''alerte hiérarchique',
    u.id_utilisateur
FROM utilisateur u
WHERE u.email = 'andry.rakotoarisoa@example.mg';
-- Activités
INSERT INTO activite (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        id_service,
        id_priorite
    )
SELECT 'TACT-A1',
    'Activité test sous-activités',
    CURRENT_DATE - 30,
    CURRENT_DATE + 30,
    s.id_service,
    p.id_priorite
FROM service s,
    priorite p
WHERE s.nom = 'Gestion des Risques et Procédures'
    AND p.code = 'NORMALE';
INSERT INTO activite (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        id_service,
        id_priorite
    )
SELECT 'TACT-A2',
    'Activité test retour en cours',
    CURRENT_DATE - 60,
    CURRENT_DATE - 30,
    s.id_service,
    p.id_priorite
FROM service s,
    priorite p
WHERE s.nom = 'Gestion des Risques et Procédures'
    AND p.code = 'NORMALE';
INSERT INTO activite (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        id_service,
        id_priorite
    )
SELECT 'TACT-A3',
    'Activité test indicateur',
    CURRENT_DATE - 10,
    CURRENT_DATE + 60,
    s.id_service,
    p.id_priorite
FROM service s,
    priorite p
WHERE s.nom = 'Gestion des Risques et Procédures'
    AND p.code = 'HAUTE';
-- Historiques (TACT-A2 doit finir en EN_RETARD pour test retour)
INSERT INTO historique_activite (
        commentaire,
        date_changement,
        id_utilisateur,
        id_statut,
        id_activite
    )
SELECT 'Init',
    NOW() - INTERVAL '2 hours',
    u.id_utilisateur,
    s.id_statut,
    a.id_activite
FROM activite a,
    utilisateur u,
    statut s
WHERE a.code = 'TACT-A1'
    AND u.email = 'andry.rakotoarisoa@example.mg'
    AND s.code = 'EN_COURS';
INSERT INTO historique_activite (
        commentaire,
        date_changement,
        id_utilisateur,
        id_statut,
        id_activite
    )
SELECT 'Init',
    NOW() - INTERVAL '3 hours',
    u.id_utilisateur,
    s.id_statut,
    a.id_activite
FROM activite a,
    utilisateur u,
    statut s
WHERE a.code = 'TACT-A2'
    AND u.email = 'andry.rakotoarisoa@example.mg'
    AND s.code = 'EN_COURS';
INSERT INTO historique_activite (
        commentaire,
        date_changement,
        id_utilisateur,
        id_statut,
        id_activite
    )
SELECT 'EN_RETARD',
    NOW() - INTERVAL '1 hour',
    u.id_utilisateur,
    s.id_statut,
    a.id_activite
FROM activite a,
    utilisateur u,
    statut s
WHERE a.code = 'TACT-A2'
    AND u.email = 'andry.rakotoarisoa@example.mg'
    AND s.code = 'EN_RETARD';
INSERT INTO historique_activite (
        commentaire,
        date_changement,
        id_utilisateur,
        id_statut,
        id_activite
    )
SELECT 'Init',
    NOW() - INTERVAL '2 hours',
    u.id_utilisateur,
    s.id_statut,
    a.id_activite
FROM activite a,
    utilisateur u,
    statut s
WHERE a.code = 'TACT-A3'
    AND u.email = 'andry.rakotoarisoa@example.mg'
    AND s.code = 'EN_COURS';
-- Affectations activités
INSERT INTO affectation_activite (
        id_activite,
        id_role,
        id_utilisateur,
        date_affectation
    )
SELECT a.id_activite,
    r.id_role,
    u.id_utilisateur,
    NOW()
FROM activite a,
    role r,
    utilisateur u
WHERE a.code = 'TACT-A1'
    AND r.code = 'RESPONSABLE'
    AND u.email = 'miora.ravelomanantsoa@example.mg';
INSERT INTO affectation_activite (
        id_activite,
        id_role,
        id_utilisateur,
        date_affectation
    )
SELECT a.id_activite,
    r.id_role,
    u.id_utilisateur,
    NOW()
FROM activite a,
    role r,
    utilisateur u
WHERE a.code = 'TACT-A1'
    AND r.code = 'SUPPLEANT'
    AND u.email = 'hery.randrianarisoa@example.mg';
INSERT INTO affectation_activite (
        id_activite,
        id_role,
        id_utilisateur,
        date_affectation
    )
SELECT a.id_activite,
    r.id_role,
    u.id_utilisateur,
    NOW()
FROM activite a,
    role r,
    utilisateur u
WHERE a.code = 'TACT-A1'
    AND r.code = 'PARTICIPANT'
    AND u.email = 'fara.razafindrakoto@example.mg';
INSERT INTO affectation_activite (
        id_activite,
        id_role,
        id_utilisateur,
        date_affectation
    )
SELECT a.id_activite,
    r.id_role,
    u.id_utilisateur,
    NOW()
FROM activite a,
    role r,
    utilisateur u
WHERE a.code = 'TACT-A3'
    AND r.code = 'RESPONSABLE'
    AND u.email = 'toky.andriambololona@example.mg';
INSERT INTO affectation_activite (
        id_activite,
        id_role,
        id_utilisateur,
        date_affectation
    )
SELECT a.id_activite,
    r.id_role,
    u.id_utilisateur,
    NOW()
FROM activite a,
    role r,
    utilisateur u
WHERE a.code = 'TACT-A3'
    AND r.code = 'SUPPLEANT'
    AND u.email = 'haja.randriamampianina@example.mg';
-- Sous-activités
INSERT INTO sous_activite (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        date_debut_reelle,
        id_activite
    )
SELECT 'TSA-A1-01',
    'Échéance dans 3 jours',
    CURRENT_DATE - 10,
    CURRENT_DATE + 3,
    CURRENT_DATE - 5,
    a.id_activite
FROM activite a
WHERE a.code = 'TACT-A1';
INSERT INTO sous_activite (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        id_activite
    )
SELECT 'TSA-A1-02',
    'Démarrage en retard',
    CURRENT_DATE - 5,
    CURRENT_DATE + 20,
    a.id_activite
FROM activite a
WHERE a.code = 'TACT-A1';
INSERT INTO sous_activite (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        date_debut_reelle,
        id_activite
    )
SELECT 'TSA-A1-03',
    'En retard',
    CURRENT_DATE - 30,
    CURRENT_DATE - 5,
    CURRENT_DATE - 25,
    a.id_activite
FROM activite a
WHERE a.code = 'TACT-A1';
INSERT INTO sous_activite (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        date_debut_reelle,
        date_fin_reelle,
        id_activite
    )
SELECT 'TSA-A1-04',
    'Terminée',
    CURRENT_DATE - 30,
    CURRENT_DATE - 5,
    CURRENT_DATE - 25,
    CURRENT_DATE - 3,
    a.id_activite
FROM activite a
WHERE a.code = 'TACT-A1';
INSERT INTO sous_activite (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        date_debut_reelle,
        id_activite
    )
SELECT 'TSA-A1-05',
    'Déjà EN_RETARD',
    CURRENT_DATE - 40,
    CURRENT_DATE - 10,
    CURRENT_DATE - 35,
    a.id_activite
FROM activite a
WHERE a.code = 'TACT-A1';
-- Affectations SA
INSERT INTO affectation_sous_activite (
        id_sous_activite,
        id_role,
        id_utilisateur,
        date_affectation
    )
SELECT sa.id_sous_activite,
    r.id_role,
    u.id_utilisateur,
    NOW()
FROM sous_activite sa,
    role r,
    utilisateur u
WHERE sa.code IN (
        'TSA-A1-01',
        'TSA-A1-02',
        'TSA-A1-03',
        'TSA-A1-04',
        'TSA-A1-05'
    )
    AND r.code = 'RESPONSABLE'
    AND u.email = 'nantenaina.rakotomalala@example.mg';
-- Avancements SA
INSERT INTO avancement_sous_activite (
        valeur_pourcentage,
        date_changement,
        id_utilisateur,
        id_sous_activite,
        id_statut
    )
SELECT 30,
    NOW() - INTERVAL '2 days',
    u.id_utilisateur,
    sa.id_sous_activite,
    s.id_statut
FROM sous_activite sa,
    utilisateur u,
    statut s
WHERE sa.code = 'TSA-A1-01'
    AND u.email = 'nantenaina.rakotomalala@example.mg'
    AND s.code = 'EN_COURS';
INSERT INTO avancement_sous_activite (
        valeur_pourcentage,
        date_changement,
        id_utilisateur,
        id_sous_activite,
        id_statut
    )
SELECT 0,
    NOW() - INTERVAL '10 days',
    u.id_utilisateur,
    sa.id_sous_activite,
    s.id_statut
FROM sous_activite sa,
    utilisateur u,
    statut s
WHERE sa.code = 'TSA-A1-02'
    AND u.email = 'nantenaina.rakotomalala@example.mg'
    AND s.code = 'NON_COMMENCEE';
INSERT INTO avancement_sous_activite (
        valeur_pourcentage,
        date_changement,
        id_utilisateur,
        id_sous_activite,
        id_statut
    )
SELECT 60,
    NOW() - INTERVAL '5 days',
    u.id_utilisateur,
    sa.id_sous_activite,
    s.id_statut
FROM sous_activite sa,
    utilisateur u,
    statut s
WHERE sa.code = 'TSA-A1-03'
    AND u.email = 'nantenaina.rakotomalala@example.mg'
    AND s.code = 'EN_COURS';
INSERT INTO avancement_sous_activite (
        valeur_pourcentage,
        date_changement,
        id_utilisateur,
        id_sous_activite,
        id_statut
    )
SELECT 100,
    NOW() - INTERVAL '3 days',
    u.id_utilisateur,
    sa.id_sous_activite,
    s.id_statut
FROM sous_activite sa,
    utilisateur u,
    statut s
WHERE sa.code = 'TSA-A1-04'
    AND u.email = 'nantenaina.rakotomalala@example.mg'
    AND s.code = 'TERMINEE';
INSERT INTO avancement_sous_activite (
        valeur_pourcentage,
        date_changement,
        id_utilisateur,
        id_sous_activite,
        id_statut
    )
SELECT 50,
    NOW() - INTERVAL '8 days',
    u.id_utilisateur,
    sa.id_sous_activite,
    s.id_statut
FROM sous_activite sa,
    utilisateur u,
    statut s
WHERE sa.code = 'TSA-A1-05'
    AND u.email = 'nantenaina.rakotomalala@example.mg'
    AND s.code = 'EN_RETARD';
-- Plans d'action
INSERT INTO plan_action (
        code,
        designation,
        date_debut_prevue,
        date_fin_prevue,
        date_debut_reelle
    )
VALUES (
        'TPA-01',
        'Plan test échéance proche',
        CURRENT_DATE - 10,
        CURRENT_DATE + 3,
        CURRENT_DATE - 5
    ),
    (
        'TPA-02',
        'Plan test retard avec incident',
        CURRENT_DATE - 40,
        CURRENT_DATE - 15,
        CURRENT_DATE - 35
    ),
    (
        'TPA-03',
        'Plan test démarrage en retard',
        CURRENT_DATE - 5,
        CURRENT_DATE + 20,
        NULL
    );
INSERT INTO affectation_plan_action (
        id_plan_action,
        id_role,
        id_utilisateur,
        date_affectation
    )
SELECT pa.id_plan_action,
    r.id_role,
    u.id_utilisateur,
    NOW()
FROM plan_action pa,
    role r,
    utilisateur u
WHERE pa.code IN ('TPA-01', 'TPA-02', 'TPA-03')
    AND r.code = 'RESPONSABLE'
    AND u.email = 'toky.andriambololona@example.mg';
INSERT INTO avancement_plan_action (
        valeur_pourcentage,
        date_changement,
        id_utilisateur,
        id_statut,
        id_plan_action
    )
SELECT 40,
    NOW() - INTERVAL '2 days',
    u.id_utilisateur,
    s.id_statut,
    pa.id_plan_action
FROM plan_action pa,
    utilisateur u,
    statut s
WHERE pa.code = 'TPA-01'
    AND u.email = 'toky.andriambololona@example.mg'
    AND s.code = 'EN_COURS';
INSERT INTO avancement_plan_action (
        valeur_pourcentage,
        date_changement,
        id_utilisateur,
        id_statut,
        id_plan_action
    )
SELECT 70,
    NOW() - INTERVAL '5 days',
    u.id_utilisateur,
    s.id_statut,
    pa.id_plan_action
FROM plan_action pa,
    utilisateur u,
    statut s
WHERE pa.code = 'TPA-02'
    AND u.email = 'toky.andriambololona@example.mg'
    AND s.code = 'EN_COURS';
INSERT INTO plan_action_origine (id_plan_action, id_origine, est_principale)
SELECT pa.id_plan_action,
    o.id_origine,
    TRUE
FROM plan_action pa,
    origine o
WHERE pa.code = 'TPA-02'
    AND o.code = 'TORI-INC-01';
-- Indicateur
INSERT INTO indicateur (code, designation, seuil_min, seuil_max, actif)
VALUES (
        'TIND-01',
        'Indicateur test seuil',
        10,
        100,
        TRUE
    );
INSERT INTO activite_indicateur (id_activite, id_indicateur)
SELECT a.id_activite,
    i.id_indicateur
FROM activite a,
    indicateur i
WHERE a.code = 'TACT-A3'
    AND i.code = 'TIND-01';
-- Valeurs d'indicateur (le trigger se déclenchera si triggers.sql est déjà chargé)
INSERT INTO valeur_indicateur (
        valeur,
        periode_debut,
        periode_fin,
        id_utilisateur,
        id_indicateur
    )
SELECT 5,
    NOW() - INTERVAL '30 days',
    NOW(),
    u.id_utilisateur,
    i.id_indicateur
FROM utilisateur u,
    indicateur i
WHERE u.email = 'toky.andriambololona@example.mg'
    AND i.code = 'TIND-01';
INSERT INTO valeur_indicateur (
        valeur,
        periode_debut,
        periode_fin,
        id_utilisateur,
        id_indicateur
    )
SELECT 150,
    NOW() - INTERVAL '60 days',
    NOW() - INTERVAL '30 days',
    u.id_utilisateur,
    i.id_indicateur
FROM utilisateur u,
    indicateur i
WHERE u.email = 'toky.andriambololona@example.mg'
    AND i.code = 'TIND-01';
INSERT INTO valeur_indicateur (
        valeur,
        periode_debut,
        periode_fin,
        id_utilisateur,
        id_indicateur
    )
SELECT 50,
    NOW() - INTERVAL '90 days',
    NOW() - INTERVAL '60 days',
    u.id_utilisateur,
    i.id_indicateur
FROM utilisateur u,
    indicateur i
WHERE u.email = 'toky.andriambololona@example.mg'
    AND i.code = 'TIND-01';

-- Initialiser les compteurs après les données de démonstration. La migration
-- équivalente initialise les bases déjà existantes lors de leur mise à niveau.
INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'ACT:PTA:' || a.id_service || ':' || code_parts[1] || ':' || a.id_objectif_specifique,
             MAX(code_parts[3]::INTEGER)
FROM activite a
JOIN objectif_specifique os ON os.id_objectif_specifique = a.id_objectif_specifique
CROSS JOIN LATERAL regexp_match(a.code, '^A-([0-9]{4})-([0-9]+)-([0-9]+)$') code_parts
CROSS JOIN LATERAL regexp_match(os.code, '([0-9]+)$') objectif_parts
WHERE code_parts IS NOT NULL
    AND objectif_parts IS NOT NULL
    AND code_parts[2]::INTEGER = objectif_parts[1]::INTEGER
GROUP BY code_parts[1], a.id_service, a.id_objectif_specifique
ON CONFLICT (cle) DO UPDATE
SET dernier_numero = GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero);

INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'ACT:NON_PTA:' || a.id_service || ':' || code_parts[1], MAX(code_parts[2]::INTEGER)
FROM activite a
CROSS JOIN LATERAL regexp_match(a.code, '^A-([0-9]{4})-([0-9]+)$') code_parts
WHERE a.id_objectif_specifique IS NULL
    AND code_parts IS NOT NULL
GROUP BY a.id_service, code_parts[1]
ON CONFLICT (cle) DO UPDATE
SET dernier_numero = GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero);

INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'SA:' || parent.id_activite,
             MAX(substring(sa.code FROM char_length('SA-' || substring(parent.code FROM 3) || '-') + 1)::INTEGER)
FROM sous_activite sa
JOIN activite parent ON parent.id_activite = sa.id_activite
WHERE left(sa.code, char_length('SA-' || substring(parent.code FROM 3) || '-'))
                = 'SA-' || substring(parent.code FROM 3) || '-'
    AND substring(sa.code FROM char_length('SA-' || substring(parent.code FROM 3) || '-') + 1) ~ '^[0-9]+$'
GROUP BY parent.id_activite
ON CONFLICT (cle) DO UPDATE
SET dernier_numero = GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero);
COMMIT;