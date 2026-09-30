-- ============================================================
-- SCRIPT DE RÉINITIALISATION DES DONNÉES
-- Base : PostgreSQL
-- Méthode : TRUNCATE avec RESTART IDENTITY et CASCADE
-- ⚠️ ATTENTION : Ce script supprime TOUTES les données
-- ============================================================

BEGIN;

-- ============================================================
-- 1. TABLES DE LIAISON (Many-to-Many / jonctions)
-- ============================================================
TRUNCATE TABLE plan_action_origine RESTART IDENTITY CASCADE;
TRUNCATE TABLE activite_indicateur RESTART IDENTITY CASCADE;
TRUNCATE TABLE poste_permission RESTART IDENTITY CASCADE;
TRUNCATE TABLE poste_utilisateur RESTART IDENTITY CASCADE;
TRUNCATE TABLE type_activite_service RESTART IDENTITY CASCADE;

-- ============================================================
-- 2. TABLES TRANSACTIONNELLES / HISTORIQUES / FICHIERS
-- ============================================================
TRUNCATE TABLE token_auth RESTART IDENTITY CASCADE;
TRUNCATE TABLE fichier_sous_activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE avancement_sous_activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE historique_activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE notification RESTART IDENTITY CASCADE;
TRUNCATE TABLE validation_plan_action RESTART IDENTITY CASCADE;
TRUNCATE TABLE avancement_plan_action RESTART IDENTITY CASCADE;
TRUNCATE TABLE affectation_plan_action RESTART IDENTITY CASCADE;
TRUNCATE TABLE origine RESTART IDENTITY CASCADE;
TRUNCATE TABLE plan_action RESTART IDENTITY CASCADE;
TRUNCATE TABLE valeur_indicateur RESTART IDENTITY CASCADE;
TRUNCATE TABLE indicateur RESTART IDENTITY CASCADE;
TRUNCATE TABLE validation_activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE etape_validation RESTART IDENTITY CASCADE;
TRUNCATE TABLE "procedure" RESTART IDENTITY CASCADE;
TRUNCATE TABLE affectation_sous_activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE permission RESTART IDENTITY CASCADE;
TRUNCATE TABLE role RESTART IDENTITY CASCADE;
TRUNCATE TABLE type_token RESTART IDENTITY CASCADE;

-- ============================================================
-- 3. TABLES MÉTIER (activités / sous-activités / livrables)
-- ============================================================
TRUNCATE TABLE livrable_sous_activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE resultat_intermediaire RESTART IDENTITY CASCADE;
TRUNCATE TABLE sous_activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE objectif_specifique RESTART IDENTITY CASCADE;

-- ============================================================
-- 4. TABLES DE RÉFÉRENCE
-- ============================================================
TRUNCATE TABLE parametre RESTART IDENTITY CASCADE;
TRUNCATE TABLE poste RESTART IDENTITY CASCADE;
TRUNCATE TABLE priorite RESTART IDENTITY CASCADE;
TRUNCATE TABLE statut RESTART IDENTITY CASCADE;
TRUNCATE TABLE type_activite RESTART IDENTITY CASCADE;
TRUNCATE TABLE site RESTART IDENTITY CASCADE;

-- ============================================================
-- 5. UTILISATEURS, SERVICES, DÉPARTEMENTS
-- ============================================================
TRUNCATE TABLE utilisateur RESTART IDENTITY CASCADE;
TRUNCATE TABLE service RESTART IDENTITY CASCADE;
TRUNCATE TABLE departement RESTART IDENTITY CASCADE;

COMMIT;

-- ============================================================
-- VÉRIFICATION (optionnel)
-- ============================================================
-- SELECT 'departement' AS table_name, COUNT(*) FROM departement
-- UNION ALL SELECT 'service', COUNT(*) FROM service
-- UNION ALL SELECT 'utilisateur', COUNT(*) FROM utilisateur
-- UNION ALL SELECT 'activite', COUNT(*) FROM activite
-- UNION ALL SELECT 'plan_action', COUNT(*) FROM plan_action;