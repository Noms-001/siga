CREATE TYPE decision_validation AS ENUM (
    'EN_ATTENTE_VALIDATION',
    'VALIDE',
    'REJETE',
    'RETOUR_MODIFICATION'
);

CREATE TABLE departement(
   id_departement SERIAL,
   code VARCHAR(20)  NOT NULL,
   nom VARCHAR(100)  NOT NULL,
   description TEXT,
   PRIMARY KEY(id_departement),
   UNIQUE(code)
);

CREATE TABLE service(
   id_service SERIAL,
   nom VARCHAR(250)  NOT NULL,
   description TEXT,
   actif BOOLEAN NOT NULL,
   date_desactivation TIMESTAMP,
   id_departement INTEGER NOT NULL,
   PRIMARY KEY(id_service),
   UNIQUE(id_service, id_departement),
   FOREIGN KEY(id_departement) REFERENCES departement(id_departement)
);

CREATE INDEX ix_service_departement
ON service (id_departement, nom);

CREATE TABLE objectif_specifique(
   id_objectif_specifique SERIAL,
   code VARCHAR(20)  NOT NULL,
   designation VARCHAR(250)  NOT NULL,
   annee INTEGER NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_objectif_specifique),
   UNIQUE(code),
   CONSTRAINT chk_objectif_specifique_annee CHECK (annee BETWEEN 1900 AND 2100)
);

CREATE INDEX ix_objectif_specifique_annee
ON objectif_specifique (annee DESC, code);

CREATE TABLE site(
   id_site SERIAL,
   nom VARCHAR(100)  NOT NULL,
   actif BOOLEAN NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_desactivation TIMESTAMP,
   PRIMARY KEY(id_site)
);

CREATE TABLE type_activite(
   id_type_activite SERIAL,
   designation VARCHAR(100)  NOT NULL,
   description TEXT,
   actif BOOLEAN NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_desactivation TIMESTAMP,
   PRIMARY KEY(id_type_activite)
);

CREATE TABLE statut(
   id_statut SERIAL,
   code VARCHAR(50)  NOT NULL,
   libelle VARCHAR(100)  NOT NULL,
   description TEXT,
   actif BOOLEAN NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP,
   date_desactivation TIMESTAMP,
   PRIMARY KEY(id_statut),
   UNIQUE(code)
);

CREATE TABLE priorite(
   id_priorite SERIAL,
   code VARCHAR(50)  NOT NULL,
   description TEXT,
   actif BOOLEAN NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_desactivation TIMESTAMP,
   libelle VARCHAR(100)  NOT NULL,
   PRIMARY KEY(id_priorite),
   UNIQUE(code)
);

CREATE TABLE poste(
   id_poste SERIAL,
   nom VARCHAR(150)  NOT NULL,
   effectif_prevu INTEGER NOT NULL,
   effectif_reel INTEGER,
   actif BOOLEAN NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP,
   date_desactivation TIMESTAMP,
   is_metier BOOLEAN NOT NULL DEFAULT TRUE,
   PRIMARY KEY(id_poste),
   CONSTRAINT chk_poste_effectif_prevu CHECK (effectif_prevu >= 0),
   CONSTRAINT chk_poste_effectif_reel CHECK (effectif_reel IS NULL OR effectif_reel >= 0)
);

CREATE TABLE "procedure"(
   id_procedure SERIAL,
   designation VARCHAR(255)  NOT NULL,
   description TEXT,
   PRIMARY KEY(id_procedure)
);

CREATE TABLE activite(
   id_activite SERIAL,
   code VARCHAR(20)  NOT NULL,
   reference VARCHAR(20),
   designation VARCHAR(255)  NOT NULL,
   date_debut_prevue DATE NOT NULL,
   date_fin_prevue DATE,
   date_debut_reelle DATE,
   date_fin_reelle DATE,
   id_objectif_specifique INTEGER,
   id_type_activite INTEGER,
   id_site INTEGER,
   id_priorite INTEGER NOT NULL,
   id_service INTEGER NOT NULL,
   id_procedure INTEGER,
   PRIMARY KEY(id_activite),
   UNIQUE(code),
   FOREIGN KEY(id_objectif_specifique) REFERENCES objectif_specifique(id_objectif_specifique),
   FOREIGN KEY(id_type_activite) REFERENCES type_activite(id_type_activite),
   FOREIGN KEY(id_site) REFERENCES site(id_site),
   FOREIGN KEY(id_priorite) REFERENCES priorite(id_priorite),
   FOREIGN KEY(id_service) REFERENCES service(id_service),
   FOREIGN KEY(id_procedure) REFERENCES "procedure"(id_procedure),
   CONSTRAINT chk_activite_dates_prevues CHECK (
       date_fin_prevue IS NULL OR date_fin_prevue >= date_debut_prevue
   ),
   CONSTRAINT chk_activite_dates_reelles CHECK (
       date_debut_reelle IS NULL OR date_fin_reelle IS NULL OR date_fin_reelle >= date_debut_reelle
   )
);

CREATE INDEX ix_activite_service
ON activite (id_service);

CREATE INDEX ix_activite_objectif
ON activite (id_objectif_specifique);

CREATE TABLE sous_activite(
   id_sous_activite SERIAL,
   code VARCHAR(20)  NOT NULL,
   designation VARCHAR(255)  NOT NULL,
   date_debut_prevue DATE NOT NULL,
   date_fin_prevue DATE NOT NULL,
   date_debut_reelle DATE,
   date_fin_reelle DATE,
   id_activite INTEGER NOT NULL,
   PRIMARY KEY(id_sous_activite),
   UNIQUE(code),
   FOREIGN KEY(id_activite) REFERENCES activite(id_activite),
   CONSTRAINT chk_sous_activite_dates_prevues CHECK (
       date_fin_prevue >= date_debut_prevue
   ),
   CONSTRAINT chk_sous_activite_dates_reelles CHECK (
       date_debut_reelle IS NULL OR date_fin_reelle IS NULL OR date_fin_reelle >= date_debut_reelle
   )
);

CREATE INDEX ix_sous_activite_activite
ON sous_activite (id_activite, date_debut_prevue, code);

CREATE TABLE resultat_intermediaire(
   id_resultat_intermediaire SERIAL,
   designation VARCHAR(255)  NOT NULL,
   id_activite INTEGER NOT NULL,
   PRIMARY KEY(id_resultat_intermediaire),
   FOREIGN KEY(id_activite) REFERENCES activite(id_activite)
);

CREATE INDEX ix_resultat_intermediaire_activite
ON resultat_intermediaire (id_activite);

CREATE TABLE livrable_sous_activite(
   id_livrable_sous_activite SERIAL,
   designation VARCHAR(250)  NOT NULL,
   description TEXT,
   id_sous_activite INTEGER NOT NULL,
   PRIMARY KEY(id_livrable_sous_activite),
   FOREIGN KEY(id_sous_activite) REFERENCES sous_activite(id_sous_activite)
);

CREATE INDEX ix_livrable_sous_activite_sous
ON livrable_sous_activite (id_sous_activite, designation);

CREATE TABLE utilisateur(
   id_utilisateur SERIAL,
   nom VARCHAR(250)  NOT NULL,
   prenom VARCHAR(250)  NOT NULL,
   email VARCHAR(100)  NOT NULL,
   telephone VARCHAR(20),
   mot_de_passe VARCHAR(255),
   actif BOOLEAN NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_desactivation TIMESTAMP,
   date_modification TIMESTAMP,
   date_derniere_connexion TIMESTAMP,
   id_departement INTEGER,
   id_service INTEGER,
   PRIMARY KEY(id_utilisateur),
   FOREIGN KEY(id_departement) REFERENCES departement(id_departement),
   FOREIGN KEY(id_service) REFERENCES service(id_service),
   FOREIGN KEY(id_service, id_departement) REFERENCES service(id_service, id_departement),
   CONSTRAINT chk_utilisateur_service_departement CHECK (
       id_service IS NULL OR id_departement IS NOT NULL
   )
);

CREATE INDEX ix_utilisateur_email
ON utilisateur (email);

CREATE TABLE type_token(
   id_type_token SERIAL,
   code VARCHAR(100)  NOT NULL,
   libelle VARCHAR(150)  NOT NULL,
   description TEXT,
   PRIMARY KEY(id_type_token),
   UNIQUE(code)
);

CREATE TABLE role(
   id_role SERIAL,
   code VARCHAR(100)  NOT NULL,
   designation VARCHAR(150)  NOT NULL,
   description TEXT,
   actif BOOLEAN NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP,
   date_desactivation TIMESTAMP,
   PRIMARY KEY(id_role),
   UNIQUE(code)
);

CREATE TABLE permission(
   id_permission SERIAL,
   designation VARCHAR(255)  NOT NULL,
   description TEXT,
   ressource VARCHAR(100)  NOT NULL,
   action VARCHAR(50)  NOT NULL,
   actif BOOLEAN NOT NULL,
   date_desactivation TIMESTAMP,
   PRIMARY KEY(id_permission)
);

CREATE TABLE affectation_sous_activite(
   id_affectation_sous_activite SERIAL,
   date_affectation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_desaffectation TIMESTAMP,
   id_role INTEGER NOT NULL,
   id_sous_activite INTEGER NOT NULL,
   id_utilisateur INTEGER NOT NULL,
   PRIMARY KEY(id_affectation_sous_activite),
   FOREIGN KEY(id_role) REFERENCES role(id_role),
   FOREIGN KEY(id_sous_activite) REFERENCES sous_activite(id_sous_activite),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE INDEX ix_affectation_sous_activite_sous
ON affectation_sous_activite (id_sous_activite, date_affectation DESC);

CREATE TABLE etape_validation(
   id_etape_validation SERIAL,
   designation VARCHAR(255)  NOT NULL,
   description TEXT,
   niveau INTEGER NOT NULL,
   obligatoire BOOLEAN NOT NULL,
   actif BOOLEAN NOT NULL,
   date_desactivation TIMESTAMP,
   id_procedure INTEGER NOT NULL,
   retour BOOLEAN NOT NULL DEFAULT FALSE,
   PRIMARY KEY(id_etape_validation),
   FOREIGN KEY(id_procedure) REFERENCES "procedure"(id_procedure),
   CONSTRAINT chk_etape_validation_niveau CHECK (niveau > 0)
);

CREATE TABLE etape_validation_decideur(
   id_etape_validation INTEGER NOT NULL,
   id_poste INTEGER NOT NULL,
   PRIMARY KEY(id_etape_validation, id_poste),
   FOREIGN KEY(id_etape_validation) REFERENCES etape_validation(id_etape_validation),
   FOREIGN KEY(id_poste) REFERENCES poste(id_poste)
);

CREATE TABLE validation_activite(
   id_validation_activite SERIAL,
   decision decision_validation NOT NULL DEFAULT 'EN_ATTENTE_VALIDATION',
   commentaire TEXT,
   date_demande TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_decision TIMESTAMP,
   id_activite INTEGER NOT NULL,
   id_etape_validation INTEGER NOT NULL,
   id_demandeur INTEGER NOT NULL,
   id_decideur INTEGER,
   PRIMARY KEY(id_validation_activite),
   FOREIGN KEY(id_activite) REFERENCES activite(id_activite),
   FOREIGN KEY(id_etape_validation) REFERENCES etape_validation(id_etape_validation),
   FOREIGN KEY(id_demandeur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_decideur) REFERENCES utilisateur(id_utilisateur),
   CONSTRAINT chk_validation_activite_decision_date CHECK (
       (decision = 'EN_ATTENTE_VALIDATION' AND date_decision IS NULL)
       OR
       (decision IN ('VALIDE', 'REJETE', 'RETOUR_MODIFICATION') AND date_decision IS NOT NULL)
   )
);

CREATE INDEX ix_validation_activite_activite
ON validation_activite (id_activite);

CREATE TABLE indicateur(
   id_indicateur SERIAL,
   code VARCHAR(50)  NOT NULL,
   code_hopex VARCHAR(150),
   indicateur_hopex VARCHAR(250),
   actif BOOLEAN DEFAULT TRUE,
   unite_mesure VARCHAR(50),
   frequence_verification VARCHAR(50),
   frequence_aggregation VARCHAR(50),
   definition TEXT,
   methode_determination TEXT,
   objectif TEXT,
   valeur_cible NUMERIC(15,2),
   seuil_min NUMERIC(15,2),
   seuil_max NUMERIC(15,2),
   designation VARCHAR(250)  NOT NULL,
   type_indicateur VARCHAR(20),
   PRIMARY KEY(id_indicateur),
   UNIQUE(code),
   UNIQUE(code_hopex),
   UNIQUE(indicateur_hopex)
);

CREATE TABLE valeur_indicateur(
   id_valeur_indicateur SERIAL,
   valeur NUMERIC(15,2)   NOT NULL,
   periode_debut TIMESTAMP NOT NULL,
   periode_fin TIMESTAMP NOT NULL,
   commentaire TEXT,
   date_saisie TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INTEGER NOT NULL,
   id_indicateur INTEGER NOT NULL,
   PRIMARY KEY(id_valeur_indicateur),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_indicateur) REFERENCES indicateur(id_indicateur),
   CONSTRAINT chk_valeur_indicateur_periode CHECK (periode_fin > periode_debut)
);

CREATE INDEX ix_valeur_indicateur_indicateur
ON valeur_indicateur (id_indicateur);

CREATE TABLE plan_action(
   id_plan_action SERIAL,
   code VARCHAR(50)  NOT NULL,
   designation VARCHAR(255)  NOT NULL,
   date_debut_prevue DATE NOT NULL,
   date_fin_prevue DATE NOT NULL,
   date_debut_reelle DATE,
   date_fin_reelle DATE,
   PRIMARY KEY(id_plan_action),
   UNIQUE(code),
   CONSTRAINT chk_plan_action_dates_prevues CHECK (
       date_fin_prevue >= date_debut_prevue
   ),
   CONSTRAINT chk_plan_action_dates_reelles CHECK (
       date_debut_reelle IS NULL OR date_fin_reelle IS NULL OR date_fin_reelle >= date_debut_reelle
   )
);

CREATE TABLE origine(
   id_origine SERIAL,
   code VARCHAR(50)  NOT NULL,
   designation VARCHAR(255)  NOT NULL,
   type_origine VARCHAR(50)  NOT NULL,
   description TEXT,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INTEGER NOT NULL,
   PRIMARY KEY(id_origine),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE affectation_plan_action(
   id_affectation_plan_action SERIAL,
   date_affectation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_desaffectation TIMESTAMP,
   id_plan_action INTEGER NOT NULL,
   id_utilisateur INTEGER NOT NULL,
   id_role INTEGER NOT NULL,
   PRIMARY KEY(id_affectation_plan_action),
   FOREIGN KEY(id_plan_action) REFERENCES plan_action(id_plan_action),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_role) REFERENCES role(id_role)
);

CREATE TABLE avancement_plan_action(
   id_avancement_plan_action SERIAL,
   valeur_pourcentage NUMERIC(15,2)   NOT NULL,
   commentaire TEXT,
   date_changement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INTEGER NOT NULL,
   id_statut INTEGER NOT NULL,
   id_plan_action INTEGER NOT NULL,
   PRIMARY KEY(id_avancement_plan_action),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_statut) REFERENCES statut(id_statut),
   FOREIGN KEY(id_plan_action) REFERENCES plan_action(id_plan_action),
   CONSTRAINT chk_avancement_plan_action_pourcentage CHECK (
       valeur_pourcentage >= 0 AND valeur_pourcentage <= 100
   )
);

CREATE TABLE validation_plan_action(
   id_validation_plan_action SERIAL,
   decision decision_validation NOT NULL DEFAULT 'EN_ATTENTE_VALIDATION',
   commentaire TEXT,
   date_demande TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_decision TIMESTAMP,
   id_plan_action INTEGER NOT NULL,
   id_etape_validation INTEGER NOT NULL,
   id_demandeur INTEGER NOT NULL,
   id_decideur INTEGER,
   PRIMARY KEY(id_validation_plan_action),
   FOREIGN KEY(id_plan_action) REFERENCES plan_action(id_plan_action),
   FOREIGN KEY(id_etape_validation) REFERENCES etape_validation(id_etape_validation),
   FOREIGN KEY(id_demandeur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_decideur) REFERENCES utilisateur(id_utilisateur),
   CONSTRAINT chk_validation_plan_action_decision_date CHECK (
       (decision = 'EN_ATTENTE_VALIDATION' AND date_decision IS NULL)
       OR
       (decision IN ('VALIDE', 'REJETE', 'RETOUR_MODIFICATION') AND date_decision IS NOT NULL)
   )
);

CREATE TABLE notification(
   id_notification SERIAL,
   titre VARCHAR(255)  NOT NULL,
   message TEXT NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_lecture TIMESTAMP,
   id_priorite INTEGER NOT NULL,
   id_utilisateur INTEGER NOT NULL,
   PRIMARY KEY(id_notification),
   FOREIGN KEY(id_priorite) REFERENCES priorite(id_priorite),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TABLE historique_activite(
   id_historique_activite SERIAL,
   commentaire TEXT,
   date_changement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INTEGER NOT NULL,
   id_statut INTEGER NOT NULL,
   id_activite INTEGER NOT NULL,
   PRIMARY KEY(id_historique_activite),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_statut) REFERENCES statut(id_statut),
   FOREIGN KEY(id_activite) REFERENCES activite(id_activite)
);

-- L'ordre DESC reproduit celui du "NOT EXISTS (date, id) > (date, id)" qui
-- remplace le ORDER BY ... LIMIT 1 du statut courant, ainsi que celui de
-- l'historique. Ne pas le repasser en ASC : la comparaison de tuples resterait
-- correcte, mais l'index ne fournirait plus l'ordre.
CREATE INDEX ix_historique_activite_activite
ON historique_activite (id_activite, date_changement DESC, id_historique_activite DESC);

CREATE TABLE avancement_sous_activite(
   id_historique_sous_activite SERIAL,
   valeur_pourcentage NUMERIC(15,2)   NOT NULL,
   commentaire TEXT,
   date_changement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INTEGER NOT NULL,
   id_sous_activite INTEGER NOT NULL,
   id_statut INTEGER NOT NULL,
   PRIMARY KEY(id_historique_sous_activite),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_sous_activite) REFERENCES sous_activite(id_sous_activite),
   FOREIGN KEY(id_statut) REFERENCES statut(id_statut),
   CONSTRAINT chk_avancement_sous_activite_pourcentage CHECK (
       valeur_pourcentage >= 0 AND valeur_pourcentage <= 100
   )
);

-- Meme raison que ix_historique_activite_activite : dernier avancement courant
-- par sous-activite, puis historique de la sous-activite, tous deux dans le
-- meme sens de lecture.
CREATE INDEX ix_avancement_sous_activite_sous
ON avancement_sous_activite (id_sous_activite, date_changement DESC, id_historique_sous_activite DESC);

CREATE TABLE fichier_sous_activite(
   id_fichier_sous_activite SERIAL,
   nom_fichier VARCHAR(255)  NOT NULL,
   nom_original VARCHAR(255)  NOT NULL,
   chemin_fichier VARCHAR(255)  NOT NULL,
   version INTEGER NOT NULL,
   extension VARCHAR(20),
   type_mime VARCHAR(150),
   taille BIGINT,
   date_depot TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   id_utilisateur INTEGER NOT NULL,
   id_livrable_sous_activite INTEGER NOT NULL,
   PRIMARY KEY(id_fichier_sous_activite),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur),
   FOREIGN KEY(id_livrable_sous_activite) REFERENCES livrable_sous_activite(id_livrable_sous_activite),
   CONSTRAINT chk_fichier_sous_activite_version CHECK (version > 0),
   CONSTRAINT chk_fichier_sous_activite_taille CHECK (taille IS NULL OR taille >= 0)
);

CREATE INDEX ix_fichier_sous_activite_livrable
ON fichier_sous_activite (id_livrable_sous_activite);

CREATE TABLE token_auth(
   id_token_auth SERIAL,
   token VARCHAR(255)  NOT NULL,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_expiration TIMESTAMP,
   date_utilisation TIMESTAMP,
   id_type_token INTEGER NOT NULL,
   id_utilisateur INTEGER NOT NULL,
   PRIMARY KEY(id_token_auth),
   FOREIGN KEY(id_type_token) REFERENCES type_token(id_type_token),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE INDEX ix_token_auth_token
ON token_auth (token);

CREATE TABLE type_activite_service(
   id_service INTEGER,
   id_type_activite INTEGER,
   PRIMARY KEY(id_service, id_type_activite),
   FOREIGN KEY(id_service) REFERENCES service(id_service),
   FOREIGN KEY(id_type_activite) REFERENCES type_activite(id_type_activite)
);

CREATE TABLE poste_utilisateur(
   id_poste INTEGER,
   id_utilisateur INTEGER,
   PRIMARY KEY(id_poste, id_utilisateur),
   FOREIGN KEY(id_poste) REFERENCES poste(id_poste),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateur(id_utilisateur)
);

CREATE TYPE portee_permission AS ENUM (
    'UTILISATEUR',
    'SERVICE',
    'DEPARTEMENT',
    'TOUS'
);

CREATE TABLE poste_permission(
   id_poste INTEGER,
   id_permission INTEGER,
   portee portee_permission,
   PRIMARY KEY(id_poste, id_permission),
   FOREIGN KEY(id_poste) REFERENCES poste(id_poste),
   FOREIGN KEY(id_permission) REFERENCES permission(id_permission)
);

CREATE TABLE activite_indicateur(
   id_activite INTEGER,
   id_indicateur INTEGER,
   PRIMARY KEY(id_activite, id_indicateur),
   FOREIGN KEY(id_activite) REFERENCES activite(id_activite),
   FOREIGN KEY(id_indicateur) REFERENCES indicateur(id_indicateur)
);

CREATE TABLE plan_action_origine(
   id_plan_action INTEGER,
   id_origine INTEGER,
   est_principale BOOLEAN,
   PRIMARY KEY(id_plan_action, id_origine),
   FOREIGN KEY(id_plan_action) REFERENCES plan_action(id_plan_action),
   FOREIGN KEY(id_origine) REFERENCES origine(id_origine)
);

CREATE UNIQUE INDEX uq_plan_action_origine_principale
ON plan_action_origine (id_plan_action)
WHERE est_principale IS TRUE;

CREATE TABLE parametre(
   id_parametre SERIAL,
   code VARCHAR(100) NOT NULL,
   designation VARCHAR(255) NOT NULL,
   valeur TEXT NOT NULL,
   type_valeur VARCHAR(30) NOT NULL,
   description TEXT,
   categorie VARCHAR(100),
   modifiable BOOLEAN NOT NULL DEFAULT TRUE,
   actif BOOLEAN NOT NULL DEFAULT TRUE,
   date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   date_modification TIMESTAMP,
   date_desactivation TIMESTAMP,
   id_utilisateur_modification INTEGER,
   PRIMARY KEY(id_parametre),
   UNIQUE(code),
   FOREIGN KEY(id_utilisateur_modification)
      REFERENCES utilisateur(id_utilisateur),
   CHECK(type_valeur IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME')),
   CHECK(categorie IS NULL OR LENGTH(TRIM(categorie)) > 0)
);