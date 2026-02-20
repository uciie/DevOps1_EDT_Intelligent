FROM node:22-alpine

WORKDIR /app

# Copie des fichiers de dépendances
COPY package*.json ./

# Installation propre
RUN npm install

# Copie du code
COPY . .

# Port Vite
EXPOSE 5173

CMD ["npm", "run", "dev", "--", "--host"]