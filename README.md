# SAGA

Application de gestion (Spring Boot + Vue.js + PostgreSQL) conteneurisée avec Docker Compose.

## Architecture

| Service    | Conteneur         | Image / Build        | Port hôte | Rôle                        |
| ---------- | ----------------- | -------------------- | --------- | --------------------------- |
| `postgres` | `saga-postgres`   | `postgres:18.4`      | 5433      | Base de données             |
| `backend`  | `saga-backend`    | `./backend`          | 8080      | API Spring Boot (Java 25)  |
| `frontend` | `saga-frontend`   | `./frontend`         | 5173      | SPA Vue.js servie par Nginx |

- Réseau : `saga-network` (bridge)
- Volume : `postgres_data` (données PostgreSQL persistées)
- URLs : http://localhost:5173 (front) · http://localhost:8080 (API)

## Prérequis

- Docker & Docker Compose v2 (`docker compose version`)
- Un fichier `.env` à la racine (voir `.env.example` ou le modèle ci-dessous)

## Variables d'environnement

Le fichier `.env` à la racine alimente `docker-compose.yml` :

```env
POSTGRES_DB=sagadb
POSTGRES_USER=saga_user
POSTGRES_PASSWORD=saga_pwd
POSTGRES_PORT=5433

BACKEND_PORT=8080
FRONTEND_PORT=5173
```

> `.env` est ignoré par git : ne jamais versionner les identifiants réels
> (`MAIL_USERNAME`, `MAIL_PASSWORD`, mots de passe BDD).

---

## Commandes Docker utiles

### Démarrage

```bash
# Construire les images et démarrer tous les services en arrière-plan
docker compose up -d --build

# Démarrer sans reconstruire les images (si déjà buildées)
docker compose up -d

# Démarrer et suivre les logs en direct (mode foreground, Ctrl+C pour arrêter)
docker compose up --build

# Démarrer uniquement certains services
docker compose up -d postgres          # base de données seule
docker compose up -d backend           # API seule (démarre postgres via depends_on)
docker compose up -d --build backend   # rebuild + relancer uniquement l'API
```

### Logs

```bash
# Logs de tous les services
docker compose logs -f

# Logs d'un seul service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres

# Dernières 100 lignes, sans follow
docker compose logs --tail=100 backend
```

### État des services

```bash
# Liste des conteneurs et de leur état
docker compose ps

# Vérifier la healthcheck PostgreSQL (doit afficher "healthy")
docker inspect saga-postgres --format '{{.State.Health.Status}}'
```

### Accès aux conteneurs

```bash
# Shell dans un conteneur
docker compose exec backend sh
docker compose exec frontend sh
docker compose exec postgres bash

# Shell interactif (le "-it" est requis pour rester dans le shell)
docker compose exec -it postgres bash
```

### Connexion à PostgreSQL

```bash
# Ouvrir psql avec les identifiants du .env
docker compose exec postgres psql -U saga_user -d sagadb

# Exécuter une requête sans entrer dans psql
docker compose exec postgres psql -U saga_user -d sagadb -c "\dt"

# Appliquer le script de réinitialisation (⚠️ supprime toutes les données)
docker compose exec -T postgres psql -U saga_user -d sagadb < database/script/reset.sql
```

### Reconstruire / redémarrer

```bash
# Reconstruire une image sans cache (dépendances Maven/npm re-téléchargées)
docker compose build --no-cache backend
docker compose build --no-cache frontend

# Reconstruire puis recréer un service
docker compose up -d --build backend

# Redémarrer un service
docker compose restart backend

# Forcer la recréation des conteneurs (après un changement de .env par ex.)
docker compose up -d --force-recreate
```

### Arrêt

```bash
# Arrêter les conteneurs (les données et les images sont conservées)
docker compose down

# Arrêter et supprimer aussi le réseau
docker compose down

# Arrêter + supprimer le volume postgres_data (⚠️ SUPPRIME LA BASE)
docker compose down -v

# Arrêter puis relancer
docker compose restart
```

### Nettoyage

```bash
# Supprimer les conteneurs et réseaux arrêtés
docker container prune

# Supprimer les images non utilisées
docker image prune -a

# Nettoyer tout (attention, requis souvent -v pour les volumes)
docker system prune -a
```

### Diagnostic

```bash
# Lister les images construites
docker images

# Événements Docker en temps réel
docker events

# Ressources utilisées par les conteneurs
docker stats
```

---

## Base de données

Scripts dans `database/script/` :

| Fichier      | Rôle                                                            |
| ------------ | --------------------------------------------------------------- |
| `schema.sql` | Création des tables (534 lignes)                                 |
| `data.sql`   | Jeu de données initial (1488 lignes)                             |
| `reset.sql`  | Réinitialisation : `TRUNCATE ... RESTART IDENTITY CASCADE`        |

> ⚠️ `schema.sql` et `data.sql` ne sont exécutés **qu'à la première création**
> du volume `postgres_data` (via `/docker-entrypoint-initdb.d/`). Pour les
> rejouer après modification :
>
> ```bash
> docker compose down -v
> docker compose up -d
> ```
>
> Ou manuellement :
>
> ```bash
> docker compose exec -T postgres psql -U saga_user -d sagadb < database/script/schema.sql
> docker compose exec -T postgres psql -U saga_user -d sagadb < database/script/data.sql
> ```

## Exécution locale sans Docker

Seuls le backend et le frontend tournent en local ; la base de données peut
rester dans Docker ou être installée nativement.

### Prérequis

| Outil     | Version requise            |
| --------- | -------------------------- |
| JDK       | **25** (`java.version` du `pom.xml`) |
| Maven     | 3.9+ (ou utiliser `./mvnw`) |
| Node.js   | `^22.18.0` ou `>=24.12.0` (voir `engines` du `package.json`) |
| PostgreSQL | 18.x (ou Docker)           |

Vérification :

```bash
java -version    # doit afficher 25
node -v          # doit afficher v22.18+ ou v24.12+
```

### 1. Base de données

#### Option A — PostgreSQL natif

```bash
# Se connecter en administrateur (superuser du système)
sudo -u postgres psql

# Créer le rôle et la base
psql> CREATE USER saga_user WITH PASSWORD 'saga_pwd';
psql> CREATE DATABASE sagadb OWNER saga_user;
psql> \q

# Charger le schéma puis les données de démonstration
cd database/script
psql -h localhost -U saga_user -d sagadb -f schema.sql
psql -h localhost -U saga_user -d sagadb -f data.sql
```

#### Option B — PostgreSQL dans Docker

```bash
# Démarrer uniquement la base (port exposé : 5433)
docker compose up -d postgres

# Le schéma et les données sont appliqués automatiquement au premier démarrage.
# En cas de modification des scripts, forcer la réinitialisation :
docker compose down -v && docker compose up -d postgres
```

### 2. Variables d'environnement du backend

> ⚠️ **Spring Boot ne lit pas le fichier `.env` du projet.** Seul Docker Compose
> injecte ces variables dans le conteneur. En local, il faut les **exporter**
> dans le shell (ou les définir dans la configuration de l'IDE).

```bash
export DB_HOST=localhost
export DB_PORT=5432        # 5433 si PostgreSQL tourne dans Docker
export DB_NAME=sagadb
export DB_USERNAME=saga_user
export DB_PASSWORD=saga_pwd

# Obligatoires : sans elles le démarrage échoue (aucune valeur par défaut)
export MAIL_USERNAME=xxxx@gmail.com
export MAIL_PASSWORD=xxxx    # mot de passe d'application Gmail

./mvnw spring-boot:run
```

L'API est disponible sur **http://localhost:8080** (aucun `server.port` défini
dans `application.properties`, 8080 est la valeur par défaut de Spring Boot).

> ℹ️ `MAIL_PASSWORD` doit être un **mot de passe d'application Google**
> (Compte Google → Sécurité → Validation en 2 étapes → Mots de passe
> d'application), et non le mot de passe du compte.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

- Serveur de dev : **http://localhost:5173**
- L'URL de l'API est lue dans `frontend/.env` : `VITE_API_URL=http://localhost:8080/api`
  (cf. `frontend/src/services/api-client.ts`)
- ⚠️ `frontend/.env` est ignoré par git (`.env` dans `.gitignore`) : sur un
  nouveau clone, le créer à la main, sinon `import.meta.env.VITE_API_URL` vaut
  `undefined` et l'API n'est pas joignable.

  ```bash
  echo "VITE_API_URL=http://localhost:8080/api" > frontend/.env
  ```
- L'adresse doit rester sur le port **5173** : le CORS du backend
  (`SecurityConfig.java`) n'autorise que `http://localhost:5173`

### Démarrage en une commande (2 terminaux)

```bash
# Terminal 1 — API
cd backend && ./mvnw spring-boot:run

# Terminal 2 — Frontend
cd frontend && npm run dev
```

### Points d'attention

- **`ddl-auto=validate`** : Hibernate ne crée aucune table. Si le schéma n'est
  pas chargé, le backend échoue au démarrage. Toujours appliquer `schema.sql`
  avant.
- **`spring.sql.init.mode=never`** : le backend n'exécute aucun script SQL au
  démarrage.
- **DevTools** : `spring-boot-devtools` est présent, le rechargement à chaud est
  automatique après un changement de fichier Java.
- Pour rebuild la BDD : `database/script/reset.sql` (supprime toutes les
  données via `TRUNCATE ... RESTART IDENTITY CASCADE`).

### Lint / tests / build

```bash
# Frontend
cd frontend
npm run lint
npm run type-check
npm run test:unit
npm run build-only

# Backend
cd backend
./mvnw clean package
```
