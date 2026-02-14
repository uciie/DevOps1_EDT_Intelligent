### Étape 1 : Accéder à Google AI Studio
Rendez-vous sur la plateforme [Google AI Studio](https://aistudio.google.com)   
Connectez-vous avec votre compte Google.

---

### Étape 2 : Créer la clé d'API
1. Cliquez sur **Get API key** (en bas à gauche de la page).
2. Cliquez sur **Créer une clé API**
3. Donnez un nom à votre clé
4. Choisissez un projet importé
5. Cliquez sur **Créer une clé**

Votre clé API Google AI Studio est prête à être utilisée.

---

### Étape 3 : Configuration du Projet 

Une fois les clés obtenues, vous devez les reporter dans le fichier de `.env` du backend.

Ouvrez le fichier `backend/.env` et complétez les champs suivants :

```properties
DB_URL=jdbc:postgresql://<votre-host>/neondb?sslmode=require
DB_USER=<votre-user>
DB_PASSWORD=<votre-password>
# Clé API : contacter l'équipe pour l'accès ou utiliser votre propre clé
# Google Maps API 
GOOGLE_MAPS_API_KEY=VOTRE_CLE_GOOGLE
SPRING_PROFILES=external-api

# Gemini AI API
CHATBOT_API_KEY=VOTRE_CLE_GOOGLE_AI

# Google OAuth 2.0
VOTRE_CLIENT_ID=VOTRE_CLE_GOOGLE_CLIENT
VOTRE_SECRET_CLIENT=VOTRE_CLE_GOOGLE_CLIENT_SECRET

```

Faite de même dans le `.env` du frontend.

Ouvrez le fichier `frontend/.env` et complétez les champs suivants :

```properties
# les même que celles du backend
VITE_GOOGLE_MAPS_API_KEY=VOTRE_CLE_GOOGLE
VITE_CHATBOT_API_KEY=VOTRE_CLE_GOOGLE_AI 
VITE_GOOGLE_CLIENT_ID=VOTRE_CLE_GOOGLE_CLIENT
VITE_GOOGLE_REDIRECT_URI=VOTRE_CLE_GOOGLE_URI
```

---
*Ce document fait partie de la documentation technique du projet DevOps1_EDT_Intelligent.*