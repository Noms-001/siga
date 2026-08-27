-- ============================================================
-- SIGA - data.sql
-- Données de démonstration
-- Les clés primaires sont générées automatiquement par PostgreSQL
-- ============================================================

BEGIN;

-- ============================================================
-- 1. DÉPARTEMENTS
-- ============================================================

INSERT INTO departement (nom, description) VALUES
('Direction des Systèmes',
 'Département en charge des systèmes et du numérique'),

('Département Administratif',
 'Département en charge de la gestion administrative');


-- ============================================================
-- 2. SERVICES
-- ============================================================

INSERT INTO service (
    id_departement,
    nom,
    description,
    actif,
    date_creation
) VALUES
(
    (SELECT id_departement
     FROM departement
     WHERE nom = 'Direction des Systèmes'),

    'Service Études et Procédures',
    'Service chargé des études et procédures',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_departement
     FROM departement
     WHERE nom = 'Direction des Systèmes'),

    'Service Informatique',
    'Service chargé des infrastructures et applications',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_departement
     FROM departement
     WHERE nom = 'Département Administratif'),

    'Service Administratif et Financier',
    'Service chargé de la gestion administrative et financière',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 3. POSTES
-- ============================================================

INSERT INTO poste (
    libelle,
    type_contrat,
    categorie,
    effectif_prevu,
    effectif_reel,
    actif,
    date_creation
) VALUES
(
    'Chef de département',
    'CDI',
    'Cadre',
    3,
    2,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Chef de service',
    'CDI',
    'Cadre',
    8,
    6,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Agent',
    'CDI',
    'Agent',
    30,
    24,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Directeur',
    'CDI',
    'Direction',
    3,
    2,
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. PRIORITÉS
-- ============================================================

INSERT INTO priorite (
    code,
    libelle,
    description,
    actif,
    date_creation
) VALUES
(
    'BASSE',
    'Basse',
    'Priorité faible',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'NORMALE',
    'Normale',
    'Priorité normale',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'HAUTE',
    'Haute',
    'Priorité importante',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'URGENTE',
    'Urgente',
    'Traitement prioritaire',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 5. TYPES D'ACTIVITÉ
-- ============================================================

INSERT INTO type_activite (
    libelle,
    actif,
    date_creation
) VALUES
('Projet', TRUE, CURRENT_TIMESTAMP),
('Action', TRUE, CURRENT_TIMESTAMP),
('Étude', TRUE, CURRENT_TIMESTAMP),
('Contrôle', TRUE, CURRENT_TIMESTAMP);


-- ============================================================
-- 6. STATUTS
-- ============================================================

INSERT INTO statut (
    libelle,
    code,
    ordre,
    description,
    actif,
    date_creation
) VALUES
(
    'Créée',
    'CREEE',
    1,
    'Activité nouvellement créée',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'En attente de validation',
    'EN_ATTENTE_VALIDATION',
    2,
    'Activité soumise au circuit de validation',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Validée',
    'VALIDEE',
    3,
    'Validation finale obtenue',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Non commencée',
    'NON_COMMENCEE',
    4,
    'Activité validée mais non démarrée',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'En cours',
    'EN_COURS',
    5,
    'Activité en cours de réalisation',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'Terminée',
    'TERMINEE',
    6,
    'Activité terminée',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 7. TYPES DE TOKEN
-- ============================================================

INSERT INTO type_token (
    code,
    libelle,
    description
) VALUES
(
    'ACTIVATION_COMPTE',
    'Activation de compte',
    'Token utilisé pour initialiser le mot de passe et activer un compte'
),

(
    'RESET_MOT_DE_PASSE',
    'Réinitialisation du mot de passe',
    'Token utilisé pour réinitialiser un mot de passe'
);


-- ============================================================
-- 8. PERMISSIONS
-- ============================================================

INSERT INTO permission (
    code,
    nom,
    description,
    ressource,
    action,
    actif
) VALUES
(
    'GERER_UTILISATEUR',
    'Gérer les utilisateurs',
    'Créer, modifier et administrer les utilisateurs',
    'UTILISATEUR',
    'GERER',
    TRUE
),

(
    'CREER_ACTIVITE',
    'Créer une activité',
    'Permet de créer une activité',
    'ACTIVITE',
    'CREER',
    TRUE
),

(
    'MODIFIER_ACTIVITE',
    'Modifier une activité',
    'Permet de modifier une activité',
    'ACTIVITE',
    'MODIFIER',
    TRUE
),

(
    'VALIDER_ACTIVITE',
    'Valider une activité',
    'Permet de valider une activité',
    'ACTIVITE',
    'VALIDER',
    TRUE
),

(
    'CREER_INDICATEUR',
    'Créer un indicateur',
    'Permet de créer un indicateur',
    'INDICATEUR',
    'CREER',
    TRUE
),

(
    'SAISIR_VALEUR_INDICATEUR',
    'Saisir une valeur',
    'Permet de saisir une valeur d''indicateur',
    'INDICATEUR',
    'SAISIR',
    TRUE
);


-- ============================================================
-- 9. RÔLES
-- ============================================================

INSERT INTO role (
    code,
    libelle,
    description,
    actif,
    date_creation
) VALUES
(
    'RESPONSABLE',
    'Responsable',
    'Responsable principal de l''activité',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'SUPPLEANT',
    'Suppléant',
    'Remplace le responsable si nécessaire',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'PARTICIPANT',
    'Participant',
    'Participe à la réalisation de l''activité',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'CONTRIBUTEUR',
    'Contributeur',
    'Contribue à la réalisation du plan d''action',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 10. OPÉRATIONS PTA
-- ============================================================

INSERT INTO operation_pta (
    code,
    libelle,
    description,
    annee,
    date_creation
) VALUES
(
    'PTA-2026-001',
    'Amélioration des processus',
    'Amélioration continue des processus',
    2026,
    CURRENT_TIMESTAMP
),

(
    'PTA-2026-002',
    'Renforcement de la sécurité',
    'Actions liées à la sécurité du système',
    2026,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 11. SITES
-- ============================================================

INSERT INTO site (
    code,
    nom,
    latitude,
    longitude,
    actif,
    date_creation
) VALUES
(
    'SITE-TANA',
    'Siège Antananarivo',
    -18.8792,
    47.5079,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'SITE-IT',
    'Centre informatique',
    -18.9150,
    47.5250,
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 12. DÉCISIONS
-- ============================================================

INSERT INTO decision_validation (
    code,
    libelle,
    description,
    actif,
    date_creation
) VALUES
(
    'VALIDER',
    'Valider',
    'Approuver l''étape de validation',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'REFUSER',
    'Refuser',
    'Rejeter la demande',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'DEMANDER_MODIFICATION',
    'Demander une modification',
    'Retourner la demande pour correction',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 13. PROCÉDURES
-- ============================================================

INSERT INTO "procedure" (
    code,
    libelle,
    description,
    actif,
    date_creation
) VALUES
(
    'PROC-STD',
    'Validation standard',
    'Circuit standard avec chef de service puis chef de département',
    TRUE,
    CURRENT_TIMESTAMP
),

(
    'PROC-STRAT',
    'Validation stratégique',
    'Circuit renforcé avec direction',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 14. ÉTAPES DE VALIDATION
-- ============================================================

INSERT INTO etape_validation (
    id_procedure,
    libelle,
    description,
    niveau,
    id_poste,
    obligatoire,
    actif,
    date_creation
) VALUES
(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STD'),

    'Validation chef de service',
    'Première étape de validation',
    1,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Chef de service'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STD'),

    'Validation chef de département',
    'Validation finale du circuit standard',
    2,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Chef de département'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STRAT'),

    'Validation chef de service',
    'Première étape de validation stratégique',
    1,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Chef de service'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STRAT'),

    'Validation chef de département',
    'Deuxième étape de validation stratégique',
    2,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Chef de département'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_procedure
     FROM "procedure"
     WHERE code = 'PROC-STRAT'),

    'Validation direction',
    'Validation finale stratégique',
    3,

    (SELECT id_poste
     FROM poste
     WHERE libelle = 'Directeur'),

    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 15. UTILISATEURS
-- ============================================================
-- Remplacer les mots de passe de démonstration
-- par de vrais hash BCrypt.

INSERT INTO utilisateur (
    id_service,
    id_poste,
    nom,
    prenom,
    email,
    telephone,
    mot_de_passe,
    actif,
    date_creation,
    date_derniere_connexion
) VALUES
(
    (SELECT id_service FROM service
     WHERE nom = 'Service Études et Procédures'),

    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de département'),

    'RAKOTO',
    'Jean',
    'jean.rakoto@siga.local',
    '032000001',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service
     WHERE nom = 'Service Études et Procédures'),

    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de service'),

    'RABE',
    'Marie',
    'marie.rabe@siga.local',
    '032000002',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service
     WHERE nom = 'Service Études et Procédures'),

    (SELECT id_poste FROM poste
     WHERE libelle = 'Agent'),

    'RANDRIA',
    'Paul',
    'paul.randria@siga.local',
    '032000003',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service
     WHERE nom = 'Service Informatique'),

    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de service'),

    'RAZAFI',
    'Sophie',
    'sophie.razafi@siga.local',
    '032000004',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service
     WHERE nom = 'Service Informatique'),

    (SELECT id_poste FROM poste
     WHERE libelle = 'Agent'),

    'RAKOTOARISOA',
    'Hery',
    'hery.rakotoarisoa@siga.local',
    '032000005',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service
     WHERE nom = 'Service Administratif et Financier'),

    (SELECT id_poste FROM poste
     WHERE libelle = 'Agent'),

    'RAVAO',
    'Nina',
    'nina.ravao@siga.local',
    '032000006',
    '$2a$10$ayz59kwaskA0DMisU9XGKuvAm9Feh9weuHhO3.Fh8ZRY6/zB.Dd66',
    TRUE,
    CURRENT_TIMESTAMP,
    NULL
);


-- ============================================================
-- 16. PERMISSIONS DES POSTES
-- ============================================================

INSERT INTO poste_permission (
    id_poste,
    id_permission
) VALUES

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de département'),

    (SELECT id_permission FROM permission
     WHERE code = 'VALIDER_ACTIVITE')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de département'),

    (SELECT id_permission FROM permission
     WHERE code = 'CREER_INDICATEUR')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de département'),

    (SELECT id_permission FROM permission
     WHERE code = 'SAISIR_VALEUR_INDICATEUR')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de service'),

    (SELECT id_permission FROM permission
     WHERE code = 'CREER_ACTIVITE')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de service'),

    (SELECT id_permission FROM permission
     WHERE code = 'MODIFIER_ACTIVITE')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de service'),

    (SELECT id_permission FROM permission
     WHERE code = 'VALIDER_ACTIVITE')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de service'),

    (SELECT id_permission FROM permission
     WHERE code = 'CREER_INDICATEUR')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Chef de service'),

    (SELECT id_permission FROM permission
     WHERE code = 'SAISIR_VALEUR_INDICATEUR')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Agent'),

    (SELECT id_permission FROM permission
     WHERE code = 'CREER_ACTIVITE')
),

(
    (SELECT id_poste FROM poste
     WHERE libelle = 'Agent'),

    (SELECT id_permission FROM permission
     WHERE code = 'MODIFIER_ACTIVITE')
);


-- ============================================================
-- 17. TOKENS
-- ============================================================

INSERT INTO token_auth (
    id_utilisateur,
    id_type_token,
    token,
    date_creation,
    date_expiration
) VALUES
(
    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'nina.ravao@siga.local'),

    (SELECT id_type_token
     FROM type_token
     WHERE code = 'ACTIVATION_COMPTE'),

    'ACTIVATION-DEMO-001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '24 hours'
),

(
    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'paul.randria@siga.local'),

    (SELECT id_type_token
     FROM type_token
     WHERE code = 'RESET_MOT_DE_PASSE'),

    'RESET-DEMO-001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 hour'
);


-- ============================================================
-- 18. ACTIVITÉS
-- ============================================================

INSERT INTO activite (
    id_service,
    id_type_activite,
    id_priorite,
    id_site,
    id_operation_pta,
    id_procedure,
    code,
    libelle,
    date_debut_prevue,
    date_fin_prevue,
    date_debut_reelle,
    date_creation
) VALUES
(
    (SELECT id_service FROM service
     WHERE nom = 'Service Études et Procédures'),

    (SELECT id_type_activite FROM type_activite
     WHERE libelle = 'Action'),

    (SELECT id_priorite FROM priorite
     WHERE code = 'HAUTE'),

    (SELECT id_site FROM site
     WHERE code = 'SITE-TANA'),

    (SELECT id_operation_pta FROM operation_pta
     WHERE code = 'PTA-2026-001'),

    (SELECT id_procedure FROM "procedure"
     WHERE code = 'PROC-STD'),

    'ACT-2026-001',
    'Réaliser les tests mensuels du SAI',
    '2026-08-01 08:00:00',
    '2026-08-30 17:00:00',
    '2026-08-05 08:30:00',
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service
     WHERE nom = 'Service Informatique'),

    (SELECT id_type_activite FROM type_activite
     WHERE libelle = 'Projet'),

    (SELECT id_priorite FROM priorite
     WHERE code = 'URGENTE'),

    (SELECT id_site FROM site
     WHERE code = 'SITE-IT'),

    (SELECT id_operation_pta FROM operation_pta
     WHERE code = 'PTA-2026-002'),

    (SELECT id_procedure FROM "procedure"
     WHERE code = 'PROC-STRAT'),

    'ACT-2026-002',
    'Renforcer la sécurité du système',
    '2026-08-10 08:00:00',
    '2026-09-15 17:00:00',
    NULL,
    CURRENT_TIMESTAMP
),

(
    (SELECT id_service FROM service
     WHERE nom = 'Service Administratif et Financier'),

    (SELECT id_type_activite FROM type_activite
     WHERE libelle = 'Étude'),

    (SELECT id_priorite FROM priorite
     WHERE code = 'NORMALE'),

    (SELECT id_site FROM site
     WHERE code = 'SITE-TANA'),

    (SELECT id_operation_pta FROM operation_pta
     WHERE code = 'PTA-2026-001'),

    (SELECT id_procedure FROM "procedure"
     WHERE code = 'PROC-STD'),

    'ACT-2026-003',
    'Étude d''optimisation des processus',
    '2026-08-15 08:00:00',
    '2026-09-05 17:00:00',
    NULL,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 19. AFFECTATIONS ACTIVITÉ / UTILISATEUR
-- ============================================================

INSERT INTO activite_utilisateur (
    id_activite,
    id_utilisateur,
    id_role,
    date_affectation
) VALUES

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    (SELECT id_role FROM role
     WHERE code = 'RESPONSABLE'),

    CURRENT_TIMESTAMP
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'paul.randria@siga.local'),

    (SELECT id_role FROM role
     WHERE code = 'SUPPLEANT'),

    CURRENT_TIMESTAMP
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'sophie.razafi@siga.local'),

    (SELECT id_role FROM role
     WHERE code = 'PARTICIPANT'),

    CURRENT_TIMESTAMP
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-002'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'sophie.razafi@siga.local'),

    (SELECT id_role FROM role
     WHERE code = 'RESPONSABLE'),

    CURRENT_TIMESTAMP
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-002'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'hery.rakotoarisoa@siga.local'),

    (SELECT id_role FROM role
     WHERE code = 'PARTICIPANT'),

    CURRENT_TIMESTAMP
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-003'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'jean.rakoto@siga.local'),

    (SELECT id_role FROM role
     WHERE code = 'RESPONSABLE'),

    CURRENT_TIMESTAMP
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-003'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'nina.ravao@siga.local'),

    (SELECT id_role FROM role
     WHERE code = 'PARTICIPANT'),

    CURRENT_TIMESTAMP
);


-- ============================================================
-- 20. HISTORIQUE
-- ============================================================

INSERT INTO historique_activite (
    id_activite,
    id_statut,
    id_utilisateur,
    date_debut,
    date_fin,
    commentaire
) VALUES

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (SELECT id_statut FROM statut
     WHERE code = 'CREEE'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    '2026-08-01 08:00:00',
    '2026-08-01 09:00:00',
    'Activité créée'
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (SELECT id_statut FROM statut
     WHERE code = 'EN_ATTENTE_VALIDATION'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    '2026-08-01 09:00:00',
    '2026-08-02 11:00:00',
    'Activité soumise à la validation'
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (SELECT id_statut FROM statut
     WHERE code = 'VALIDEE'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'jean.rakoto@siga.local'),

    '2026-08-02 11:00:00',
    '2026-08-02 11:05:00',
    'Validation finale obtenue'
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (SELECT id_statut FROM statut
     WHERE code = 'NON_COMMENCEE'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    '2026-08-02 11:05:00',
    '2026-08-05 08:30:00',
    'Activité prête à démarrer'
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (SELECT id_statut FROM statut
     WHERE code = 'EN_COURS'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    '2026-08-05 08:30:00',
    NULL,
    'Réalisation en cours'
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-002'),

    (SELECT id_statut FROM statut
     WHERE code = 'CREEE'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'sophie.razafi@siga.local'),

    '2026-08-10 08:00:00',
    '2026-08-10 09:00:00',
    'Activité créée'
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-003'),

    (SELECT id_statut FROM statut
     WHERE code = 'CREEE'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'jean.rakoto@siga.local'),

    '2026-08-15 08:00:00',
    NULL,
    'Activité créée'
);


-- ============================================================
-- 21. RESSOURCES
-- ============================================================

INSERT INTO ressource (
    id_activite,
    designation,
    type_ressource,
    quantite,
    cout
) VALUES
(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    'Ordinateur portable',
    'Matériel',
    2,
    4500000
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    'Véhicule de service',
    'Transport',
    1,
    500000
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-002'),

    'Équipement de sécurité réseau',
    'Matériel',
    1,
    3200000
);


-- ============================================================
-- 22. INDICATEURS
-- ============================================================

INSERT INTO indicateur (
    type_indicateur,
    details,
    libelle,
    code_hopex,
    indicateur_hopex,
    objectifs,
    methode_determination,
    frequence_verification,
    frequence_aggregation,
    seuil_min,
    seuil_max,
    unite,
    commentaire,
    date_creation
) VALUES
(
    'KPI',
    'Nombre de tests réalisés chaque mois',
    'Nombre de tests mensuels du SAI',
    'KPI-SAI-001',
    'SAI_TEST_MENSUEL',
    'Réaliser au moins un test mensuel',
    'Comptage des tests réalisés',
    'Mensuelle',
    'Mensuelle',
    1,
    10,
    'test',
    'Indicateur de suivi des tests SAI',
    CURRENT_TIMESTAMP
),

(
    'KPI',
    'Taux de réalisation des activités',
    'Taux de réalisation des activités',
    'KPI-ACT-001',
    'ACT_REALISATION',
    'Atteindre au moins 90 %',
    'Nombre d''activités terminées / nombre d''activités prévues',
    'Mensuelle',
    'Mensuelle',
    90,
    100,
    '%',
    'Indicateur de performance des activités',
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 23. VALEURS DES INDICATEURS
-- ============================================================

INSERT INTO valeur_indicateur (
    id_indicateur,
    periode_debut,
    periode_fin,
    valeur,
    commentaire,
    date_saisie
) VALUES
(
    (SELECT id_indicateur FROM indicateur
     WHERE code_hopex = 'KPI-SAI-001'),

    '2026-06-01',
    '2026-06-30',
    1,
    'Test réalisé en juin',
    '2026-07-01 09:00:00'
),

(
    (SELECT id_indicateur FROM indicateur
     WHERE code_hopex = 'KPI-SAI-001'),

    '2026-07-01',
    '2026-07-31',
    2,
    'Deux tests réalisés en juillet',
    '2026-08-01 09:00:00'
),

(
    (SELECT id_indicateur FROM indicateur
     WHERE code_hopex = 'KPI-SAI-001'),

    '2026-08-01',
    '2026-08-31',
    1,
    'Un test réalisé en août',
    '2026-08-25 09:00:00'
),

(
    (SELECT id_indicateur FROM indicateur
     WHERE code_hopex = 'KPI-ACT-001'),

    '2026-08-01',
    '2026-08-31',
    75,
    'Suivi intermédiaire du mois',
    '2026-08-25 09:10:00'
);


-- ============================================================
-- 24. VALIDATIONS
-- ============================================================

INSERT INTO validation_activite (
    id_activite,
    id_etape_validation,
    id_utilisateur_demandeur,
    id_utilisateur_validateur,
    id_decision,
    date_demande,
    date_decision,
    commentaire
) VALUES

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (
        SELECT ev.id_etape_validation
        FROM etape_validation ev
        JOIN "procedure" p
          ON p.id_procedure = ev.id_procedure
        WHERE p.code = 'PROC-STD'
          AND ev.niveau = 1
    ),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    (SELECT id_decision FROM decision_validation
     WHERE code = 'VALIDER'),

    '2026-08-01 09:00:00',
    '2026-08-02 10:00:00',
    'Validation du chef de service'
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-001'),

    (
        SELECT ev.id_etape_validation
        FROM etape_validation ev
        JOIN "procedure" p
          ON p.id_procedure = ev.id_procedure
        WHERE p.code = 'PROC-STD'
          AND ev.niveau = 2
    ),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'jean.rakoto@siga.local'),

    (SELECT id_decision FROM decision_validation
     WHERE code = 'VALIDER'),

    '2026-08-02 10:15:00',
    '2026-08-02 11:00:00',
    'Validation finale du chef de département'
),

(
    (SELECT id_activite FROM activite
     WHERE code = 'ACT-2026-002'),

    (
        SELECT ev.id_etape_validation
        FROM etape_validation ev
        JOIN "procedure" p
          ON p.id_procedure = ev.id_procedure
        WHERE p.code = 'PROC-STRAT'
          AND ev.niveau = 1
    ),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'sophie.razafi@siga.local'),

    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'sophie.razafi@siga.local'),

    (SELECT id_decision FROM decision_validation
     WHERE code = 'VALIDER'),

    '2026-08-10 09:30:00',
    '2026-08-10 10:00:00',
    'Validation chef de service'
);


-- ============================================================
-- 25. NOTIFICATIONS
-- ============================================================

INSERT INTO notification (
    id_utilisateur,
    id_priorite,
    titre,
    message,
    lu,
    date_creation,
    date_lecture
) VALUES

(
    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    (SELECT id_priorite FROM priorite
     WHERE code = 'HAUTE'),

    'Validation requise',
    'L''activité ACT-2026-001 nécessite votre validation.',
    TRUE,
    '2026-08-02 09:30:00',
    '2026-08-02 09:45:00'
),

(
    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'jean.rakoto@siga.local'),

    (SELECT id_priorite FROM priorite
     WHERE code = 'HAUTE'),

    'Activité validée',
    'L''activité ACT-2026-001 a été validée.',
    TRUE,
    '2026-08-02 11:05:00',
    '2026-08-02 11:10:00'
),

(
    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'hery.rakotoarisoa@siga.local'),

    (SELECT id_priorite FROM priorite
     WHERE code = 'URGENTE'),

    'Activité urgente',
    'L''activité ACT-2026-002 est prioritaire.',
    FALSE,
    '2026-08-25 08:20:00',
    NULL
),

(
    (SELECT id_utilisateur FROM utilisateur
     WHERE email = 'paul.randria@siga.local'),

    (SELECT id_priorite FROM priorite
     WHERE code = 'NORMALE'),

    'Nouvelle affectation',
    'Vous avez été affecté à l''activité ACT-2026-001.',
    FALSE,
    '2026-08-25 08:25:00',
    NULL
);


-- ============================================================
-- 26. PLANS D'ACTION
-- ============================================================

INSERT INTO plan_action (
    code,
    intitule,
    commentaire,
    date_creation
) VALUES
(
    'PA-2026-001',
    'Amélioration de la sécurité informatique',
    'Plan issu des recommandations d''audit',
    CURRENT_TIMESTAMP
),

(
    'PA-2026-002',
    'Optimisation des procédures internes',
    'Plan d''amélioration continue',
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 27. ORIGINES
-- ============================================================

INSERT INTO origine (
    code,
    type,
    reference,
    intitule,
    description
) VALUES
(
    'ORI-001',
    'AUDIT',
    'AUD-2026-01',
    'Audit interne',
    'Constats issus de l''audit interne'
),

(
    'ORI-002',
    'RECOMMANDATION',
    'REC-2026-04',
    'Recommandation de la direction',
    'Recommandation de la direction'
),

(
    'ORI-003',
    'INCIDENT',
    'INC-2026-02',
    'Incident de sécurité',
    'Origine liée à un incident de sécurité'
);


-- ============================================================
-- 28. ORIGINES DES PLANS D'ACTION
-- ============================================================

INSERT INTO plan_action_origine (
    id_plan_action,
    id_origine,
    est_principale
) VALUES

(
    (SELECT id_plan_action
     FROM plan_action
     WHERE code = 'PA-2026-001'),

    (SELECT id_origine
     FROM origine
     WHERE code = 'ORI-001'),

    TRUE
),

(
    (SELECT id_plan_action
     FROM plan_action
     WHERE code = 'PA-2026-001'),

    (SELECT id_origine
     FROM origine
     WHERE code = 'ORI-003'),

    FALSE
),

(
    (SELECT id_plan_action
     FROM plan_action
     WHERE code = 'PA-2026-002'),

    (SELECT id_origine
     FROM origine
     WHERE code = 'ORI-002'),

    TRUE
);


-- ============================================================
-- 29. UTILISATEURS DES PLANS D'ACTION
-- ============================================================

INSERT INTO plan_action_utilisateur (
    id_plan_action,
    id_utilisateur,
    id_role
) VALUES

(
    (SELECT id_plan_action
     FROM plan_action
     WHERE code = 'PA-2026-001'),

    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'jean.rakoto@siga.local'),

    (SELECT id_role
     FROM role
     WHERE code = 'RESPONSABLE')
),

(
    (SELECT id_plan_action
     FROM plan_action
     WHERE code = 'PA-2026-001'),

    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'sophie.razafi@siga.local'),

    (SELECT id_role
     FROM role
     WHERE code = 'CONTRIBUTEUR')
),

(
    (SELECT id_plan_action
     FROM plan_action
     WHERE code = 'PA-2026-002'),

    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'marie.rabe@siga.local'),

    (SELECT id_role
     FROM role
     WHERE code = 'RESPONSABLE')
),

(
    (SELECT id_plan_action
     FROM plan_action
     WHERE code = 'PA-2026-002'),

    (SELECT id_utilisateur
     FROM utilisateur
     WHERE email = 'paul.randria@siga.local'),

    (SELECT id_role
     FROM role
     WHERE code = 'PARTICIPANT')
);


-- ============================================================
-- FIN
-- ============================================================

COMMIT;