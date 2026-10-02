# ============================
# Étape 1 : Build Vue.js
# ============================
FROM node:22-alpine AS build

WORKDIR /app

# Copier les fichiers de dépendances
COPY package.json package-lock.json ./

# Installer les dépendances
RUN npm ci

# Copier le code source
COPY . .

# Construire l'application
RUN npm run build


# ============================
# Étape 2 : Serveur Nginx
# ============================
FROM nginx:alpine

# Supprimer la configuration Nginx par défaut
RUN rm /etc/nginx/conf.d/default.conf

# Ajouter notre configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copier le build Vue
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]