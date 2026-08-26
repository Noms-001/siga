# API - Module Authentification


| Méthode | Endpoint | Fonction |
|---|---|---|
| POST | `/api/auth/login` | Authentifier l'utilisateur |
| POST | `/api/auth/logout` | Déconnecter l'utilisateur |
| GET | `/api/auth/me` | Consulter l'utilisateur connecté |
| PUT | `/api/auth/me` | Modifier les informations personnelles |
| GET | `/api/auth/session` | Vérifier la session |
| GET | `/api/auth/activation/verify` | Vérifier le token d'activation |
| POST | `/api/auth/activation` | Initialiser le mot de passe et activer le compte |
| POST | `/api/auth/password/forgot` | Demander une réinitialisation |
| GET | `/api/auth/password/reset/verify` | Vérifier le token de réinitialisation |
| POST | `/api/auth/password/reset` | Réinitialiser le mot de passe |
| PUT | `/api/auth/password` | Modifier le mot de passe |
