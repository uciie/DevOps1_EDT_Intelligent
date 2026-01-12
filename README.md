# Smart Scheduler

[![License](https://img.shields.io/github/license/uciie/DevOps1_EDT_Intelligent)](./LICENSE)
[![Version](https://img.shields.io/github/v/tag/uciie/DevOps1_EDT_Intelligent)](https://github.com/uciie/DevOps1_EDT_Intelligent/releases)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uciie_DevOps1_EDT_Intelligent&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uciie_DevOps1_EDT_Intelligent)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=uciie_DevOps1_EDT_Intelligent&metric=coverage)](https://sonarcloud.io/summary/new_code?id=uciie_DevOps1_EDT_Intelligent)
[![Build](https://github.com/uciie/DevOps1_EDT_Intelligent/actions/workflows/build.yml/badge.svg)](https://github.com/uciie/DevOps1_EDT_Intelligent/actions/workflows/build.yml)
[![Tests & SonarCloud](https://github.com/uciie/DevOps1_EDT_Intelligent/actions/workflows/test.yml/badge.svg)](https://github.com/uciie/DevOps1_EDT_Intelligent/actions/workflows/test.yml)

> **Emploi du temps intelligent** — Optimisation automatique de planning, import ICS et gestion de tâches.

---

##  Sommaire
- [Smart Scheduler](#smart-scheduler)
  - [ Sommaire](#-sommaire)
  - [ Qui sommes-nous ?](#-qui-sommes-nous-)
  - [ À propos du projet](#-à-propos-du-projet)
    - [ Public visé](#-public-visé)
    - [ Analyse concurrentielle](#️-analyse-concurrentielle)
  - [ Outils et Processus DevOps](#️-outils-et-processus-devops)
  - [ Stack Technique et Outils](#️-stack-technique-et-outils)
    - [ Prérequis Système](#-prérequis-système)
    - [☕ Backend (Java / Spring Boot)](#-backend-java--spring-boot)
    - [ Frontend (React / Vite)](#️-frontend-react--vite)
  - [ Installation et configuration](#-installation-et-configuration)
    - [1 Cloner le projet](#1️⃣-cloner-le-projet)
    - [2 Configuration Backend](#2️⃣-configuration-backend)
    - [3 Configuration Frontend](#3️⃣-configuration-frontend)
  - [ Lancement](#-lancement)
    - [Backend (API)](#backend-api)
    - [Frontend (Interface)](#frontend-interface)
  - [ Équipe](#-équipe)

---

##  Qui sommes-nous ?

Nous sommes une équipe de quatre étudiants en **Master 1 MIAGE (Méthodes Informatiques Appliquées à la Gestion des Entreprises)**, parcours **MIXTE**, promotion **2025** à l'**Université Paris Nanterre**.

Ce projet a été réalisé dans le cadre de notre cursus DevOps, avec pour objectif de mettre en œuvre une chaîne d'intégration et de déploiement continue (CI/CD) sur une application Full Stack.

---

##  À propos du projet

**Smart Scheduler** est une solution intelligente de gestion du temps. Contrairement à un agenda classique où l'utilisateur doit placer manuellement chaque événement, notre application :

1.  **Importe** vos contraintes existantes (cours, réunions) via des fichiers `.ics` (ex: ENT universitaire, Google Calendar).
2.  **Analyse** les créneaux libres.
3.  **Optimise et insère automatiquement** vos tâches à faire (To-Do List) dans les "trous" de votre emploi du temps, selon des règles de priorité et de durée.

###  Public visé
* **Étudiants :** Pour jongler entre les cours, les révisions et les projets de groupe sans conflit.
* **Professionnels indépendants :** Pour optimiser les temps de trajet et les périodes de travail profond.
* **Personnes ayant des difficultés d'organisation :** L'automatisation réduit la charge mentale liée à la planification.

###  Analyse concurrentielle

| Solution | Type | Avantages | Inconvénients | Notre approche |
| :--- | :--- | :--- | :--- | :--- |
| **Google Calendar / Outlook** | Calendrier Classique | Gratuit, universel. | Aucune automatisation. L'utilisateur doit tout placer à la main. | Automatisation du placement des tâches. |
| **Motion / Reclaim.ai** | Planificateurs IA | Très puissants, fonctionnalités avancées. | Payants (chers), complexes, propriétaires (données privées). | **Open-source**, gratuit, simple d'utilisation et transparent sur les données. |
| **Todoist / Trello** | Gestionnaires de tâches | Excellents pour lister les tâches. | Ne planifient pas *quand* faire la tâche dans l'agenda. | Fusion de la liste de tâches et de l'agenda. |

---

##  Outils et Processus DevOps

Pour garantir la qualité et la maintenabilité du code, nous avons mis en place une chaîne DevOps complète :

* **Gestion de version :** Git & GitHub (Branching model: Feature Branch Workflow).
* **Intégration Continue (CI) :**
    * **GitHub Actions :** Compilation et exécution des tests unitaires et d'intégration à chaque push.
    * **Gradle :** Automatisation du build backend.
* **Qualité du code (QA) :**
    * **SonarCloud :** Analyse statique du code, détection de bugs, "code smells" et suivi de la couverture de code.
    * **JaCoCo :** Rapport de couverture de tests Java.
* **Tests :** JUnit 5 pour les tests unitaires et d'intégration.

---

##  Stack Technique et Outils

Cette section détaille les technologies et librairies clés utilisées pour le développement, le build et les tests du projet.

###  Prérequis Système
* **Java 21** (JDK) : Nécessaire pour le backend Spring Boot.
* **Node.js 22** : Recommandé pour l'exécution du frontend React (utilisé en CI).
* **PostgreSQL** : Base de données de production.

### ☕ Backend (Java / Spring Boot)
Le backend est construit avec **Spring Boot 3.5.6** et utilise **Gradle** pour l'automatisation.

* **Framework & API :**
    * `spring-boot-starter-web` : Création des endpoints REST.
    * `spring-boot-starter-data-jpa` : Interaction avec la base de données.
    * `spring-boot-starter-validation` : Validation des données entrantes.
    * `spring-dotenv` (v4.0.0) : Gestion des variables d'environnement (.env).
* **Traitement de Données :**
    * `biweekly` (v0.6.8) : Parsing et manipulation des fichiers iCalendar (.ics).
* **Base de Données :**
    * `postgresql` : Driver pour la base de données de production.
    * `h2` : Base de données en mémoire pour les tests d'intégration.
* **Tests & Qualité :**
    * **JUnit 5** (v5.10.0) & **Mockito** (v5.6.0) : Tests unitaires.
    * **JaCoCo** (v0.8.13) : Rapport de couverture de code (Minimum requis : 70%).
    * **SonarQube** (Plugin v5.1.0) : Analyse statique et qualité du code.

###  Frontend (React / Vite)
Le frontend est une SPA (Single Page Application) développée avec **React 19** et **Vite**.

* **Cœur :**
    * `react` (v19.2.0) & `react-dom` (v19.2.0).
    * `vite` (v7.1.9) : Outil de build et serveur de développement ultra-rapide.
* **Navigation & Requêtes :**
    * `react-router-dom` (v7.9.4) : Gestion du routing côté client.
    * `axios` (v1.12.2) : Client HTTP pour communiquer avec l'API Backend.
* **Interface & Calendrier :**
    * `react-big-calendar` (v1.19.4) : Composant d'affichage de l'emploi du temps.
    * `moment` (v2.30.1) : Manipulation des dates.
    * `react-dnd` (v16.0.1) & `react-dnd-html5-backend` : Gestion du Drag & Drop pour les tâches.
    * `@react-google-maps/api` (v2.20.7) : Intégration des cartes Google Maps.
* **Tests & Linting :**
    * `vitest` (v3.2.4) : Framework de tests unitaires (compatible Jest).
    * `eslint` (v9.36.0) : Linter pour garantir la qualité du code JavaScript/React.

---

## 🧩 Installation et configuration

### 1 Cloner le projet
```bash
git clone [https://github.com/uciie/DevOps1_EDT_Intelligent.git](https://github.com/uciie/DevOps1_EDT_Intelligent.git)
cd DevOps1_EDT_Intelligent
````

### 2 Configuration Backend

Ajouter le fichier `.env` dans le dossier `backend` (ne pas le committer \!) :

```properties
# .env example
DB_URL=jdbc:postgresql://<votre-host>/neondb?sslmode=require
DB_USER=<votre-user>
DB_PASSWORD=<votre-password>
GOOGLE_MAPS_INTEGRATION_TESTS=true
# Clé API : contacter l'équipe pour l'accès
GOOGLE_MAPS_API_KEY=YOUR_KEY_HERE
SPRING_PROFILES=external-api
```

### 3 Configuration Frontend

Ajouter le fichier `.env` dans le dossier `frontend` (ne pas le committer \!) :

```properties
# .env example
VITE_GOOGLE_MAPS_API_KEY=YOUR_KEY_HERE # le même que celui du backend
```

-----
### 4 Comment Obtenir les configurations du fichier .env

DB_URL, DB_USER, DB_PASSWORD
On l'obtient en allant sur le site de Neon (Neon.tech), on se connecte avec son compte Neon (Ou on créer)
créer un nouveau projet, en haut à droite appuyer sur le bouton connect, on change ensuite le langage en java, et on obtient une ligne de texte qui contient l'URL, l'user, et le password

DB_URL devrait ressembler à : jdbc:postgresql://<something>/neondb?sslmode=require&channel_binding=require
DB_USER devrait ressembler à : neondb_owner

Pour obtenir l'api de google maps, il faut aller sur google cloud, rechercher "distance matrix api", cliquer sur "enable"/"activer", ensuite vérifier votre identité sur le site de google, et voila!

##  Lancement

### Backend (API)

```bash
cd backend
# Linux / Mac
./gradlew bootRun
# Windows
gradlew.bat bootRun
```

 API accessible sur : [http://localhost:8080](https://www.google.com/search?q=http://localhost:8080)

### Frontend (Interface)

```bash
cd frontend
npm install
npm run dev
```

 Interface accessible sur : [http://localhost:5173](https://www.google.com/search?q=http://localhost:5173)

-----

## 👥 Équipe

| Membre | GitHub | Numéro Étudiant | Rôle |
|---------|---------|----|----|
| **Lucie Pan** | [@uciie](https://github.com/uciie) | 45004162 | ? |
| **Paul Beyssac** | [@BPaulz3trei](https://github.com/BPaulz3trei) | 42006035 | ? |
| **Manda Dabo** | [@MandaDABO](https://github.com/MandaDABO) | 42012949 | ? |
| **Sylvain Huang** | [@Kusanagies](https://github.com/Kusanagies) | 41005688 | ? |

-----

> Projet universitaire M1 MIAGE 2024-2025 — Université Paris Nanterre.
