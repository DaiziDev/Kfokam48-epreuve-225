# SPA Angular — compilée par Node, servie par nginx (fichiers statiques).

# --- 1. Build ------------------------------------------------------------------
# Image Debian (glibc) plutôt qu'Alpine : les binaires natifs d'esbuild et de
# Tailwind 4 sont ceux verrouillés dans package-lock.json.
FROM node:24-slim AS build
WORKDIR /app

# Dépendances d'abord : couche en cache tant que le lockfile ne change pas
COPY package.json package-lock.json ./

# Même npm que celui qui a écrit le lockfile (champ packageManager) : un npm
# d'une autre version majeure le juge désynchronisé et npm ci échoue
RUN npm install -g "npm@$(node -p "require('./package.json').packageManager.split('@')[1]")" \
    && npm ci

COPY . .
RUN npm run build

# --- 2. Service ----------------------------------------------------------------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
