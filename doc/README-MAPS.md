### Étape 1 : Accéder à Google Cloud
Rendez-vous sur la plateforme [Google Cloud](https://console.cloud.google.com/welcome)   
Connectez-vous avec votre compte Google.

---

### Étape 2 : Créer un projet
1. Cliquez sur **Sélectionner un projet** (en haut de la page).
2. Cliquez sur **Nouveau projet**.
3. Donnez un nom à votre projet.
4. Cliquez sur **Créer**.
5. Sélectionner en tant que projet

---

### Étape 3 : Activer l’API Distance Matrix
1. Allez dans **APIs et services** → **Bibliothèque**.
2. Recherchez **Distance Matrix API**.
3. Cliquez sur l’API puis sur **Activer** (*Enable*).

---

### Étape 4 : Activer la facturation et vérifier votre identité
1. Ouvrez le menu **Facturation**.
2. Associez une carte bancaire à votre projet Google Cloud.
3. Vérifiez votre identité si Google le demande.

>  Google propose un crédit gratuit mensuel, mais la carte bancaire est obligatoire pour utiliser l’API.

---

### Étape 5 : Créer une clé API
1. Allez dans **APIs et services** → **Identifiants**.
2. Cliquez sur **Créer des identifiants**.
3. Sélectionnez **Clé API**.
4. Copiez et conservez votre clé API en lieu sûr.

Votre clé API Google Distance Matrix est prête à être utilisée.

---

### Étape 6 : Configuration du Projet 

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
---
### Dépannage
* **Quota Exceeded** : Les APIs Google ont des limites gratuites. Surveillez votre consommation dans le tableau de bord Google Cloud.

---

*Ce document fait partie de la documentation technique du projet DevOps1_EDT_Intelligent.*