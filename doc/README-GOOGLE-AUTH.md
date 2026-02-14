# 🔑 Guide de Configuration : Google API & OAuth 2.0

Ce guide détaille les étapes nécessaires pour obtenir les identifiants requis pour la synchronisation avec **Google Agenda** et l'utilisation des services **Google Maps** et **Gemini AI**.

## 1. Création d'un Projet sur Google Cloud

1. Connectez-vous à la [Google Cloud Console](https://console.cloud.google.com/).
2. Cliquez sur la liste déroulante des projets et sélectionnez **"Nouveau projet"**.
3. Nommez le projet (ex: `DevOps1-EDT-Intelligent`) et validez.

## 2. Activation des APIs

Dans le menu de gauche, allez dans **"APIs et services" > "Bibliothèque"** et activez les APIs suivantes :

* **Google Calendar API** (pour la synchronisation de l'emploi du temps).
* **Distance Matrix API** (pour le calcul des temps de trajet).
* **Generative Language API** (pour les fonctionnalités du Chatbot Gemini).

## 3. Configuration de l'écran de consentement OAuth

Avant de créer les identifiants, vous devez configurer l'écran que verront les utilisateurs :

1. Allez dans **"Écran de consentement OAuth"**.
2. Choisissez le type **"Externe"**.
3. Remplissez les informations obligatoires (Nom de l'app, email de support).
4. **Scopes (Champs d'application)** : Ajoutez les scopes nécessaires pour lire et écrire dans l'agenda :
* `.../auth/calendar.events`
* `.../auth/calendar.readonly`



## 4. Obtention des Identifiants (Credentials)

### A. Identifiants OAuth 2.0 (Pour la Synchro Agenda)

1. Allez dans l'onglet **"Identifiants"**.
2. Cliquez sur **"Créer des identifiants" > "ID de client OAuth"**.
3. Type d'application : **Application Web**.
4. **Origines JavaScript autorisées** : `http://localhost:5173` (Frontend Vite).
5. **URIs de redirection autorisés** : `http://localhost:5173/google-callback`.
6. Copiez votre **Client ID** et votre **Client Secret**.

### B. Clé API (Pour Maps et Gemini)

1. Cliquez sur **"Créer des identifiants" > "Clé API"**.
2. Copiez la clé générée.
3. *Recommandation : Restreignez cette clé aux APIs "Distance Matrix" et "Generative Language" pour plus de sécurité.*

## 5. Configuration du Projet

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

## 6. Dépannage

* **Erreur 403 (Access Not Configured)** : Vérifiez que l'API Google Calendar est bien activée dans la console.
* **Erreur de redirection** : Assurez-vous que l'URL de redirection dans la console Google correspond exactement à celle configurée dans le frontend (`/google-callback`).
* **Quota Exceeded** : Les APIs Google ont des limites gratuites. Surveillez votre consommation dans le tableau de bord Google Cloud.

---

*Ce document fait partie de la documentation technique du projet DevOps1_EDT_Intelligent.*