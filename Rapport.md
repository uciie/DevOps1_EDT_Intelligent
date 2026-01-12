# Rapport de Projet : Smart Scheduler (EDT Intelligent)

## 1. Introduction
* **Contexte :** La difficulté de la gestion du temps personnel et professionnel dans un environnement hybride.
* **Problématique :** Comment optimiser automatiquement un emploi du temps en intégrant des contraintes fixes (cours, réunions) et des tâches flexibles (To-Do List) ?
* **Solution proposée :** **Smart Scheduler**, une application Full Stack permettant l'import de calendriers, la gestion de tâches et l'optimisation automatique des créneaux libres.
* **Public visé :** Étudiants, freelances et personnes cherchant à réduire leur charge mentale organisationnelle.

## 2. Architecture Technique
### 2.1. Vue d'ensemble
* Présentation de l'architecture Client-Serveur (API REST).
* **Diagramme de classes :** Référence au diagramme UML montrant les relations entre `User`, `Event`, `Task` et `Team`.

### 2.2. Backend (Spring Boot)
* **Technologies :** Java 21, Spring Boot 3.5.6, Gradle.
* **Structure en couches :**
    * **Controllers :** Gestion des endpoints (ex: `ScheduleController`, `EventController`).
    * **Services :** Logique métier (ex: `ScheduleOptimizerService`, `TravelTimeService`).
    * **Repositories :** Accès aux données (JPA/Hibernate).
* **Gestion des données :** Utilisation de PostgreSQL en production et H2 pour les tests.

### 2.3. Frontend (React)
* **Technologies :** React 19, Vite, Axios.
* **Interface utilisateur :**
    * Utilisation de `react-big-calendar` pour la visualisation.
    * Composants clés : `Calendar`, `TodoList`, `EventForm`.

## 3. Fonctionnalités Clés et Implémentation
### 3.1. Importation et Parsing de Calendriers
* Fonctionnement du service `CalendarImportService`.
* Utilisation de la librairie `biweekly` pour traiter les fichiers `.ics`.
* Catégorisation automatique des événements importés.

### 3.2. Algorithme d'Optimisation d'Emploi du Temps
* **Cœur du projet :** Le `ScheduleOptimizerService` qui remplit les "trous" de l'agenda.
* **Stratégie :** Utilisation de `TaskSelectionStrategy` pour prioriser les tâches à placer.

### 3.3. Calcul des Temps de Trajet
* Gestion des déplacements entre deux événements (`TravelTimeService`).
* **Pattern Strategy :** Coexistence de deux implémentations via l'interface `TravelTimeCalculator` :
    * `GoogleMapsTravelTimeCalculator` (API réelle).
    * `SimpleTravelTimeCalculator` (Estimation par vol d'oiseau/vitesse moyenne pour les tests ou fallback).

### 3.4. Fonctionnalités Collaboratives
* Gestion des équipes via `TeamService`.
* Système d'invitations (`TeamInvitation`) et partage de visibilité.

## 4. Stratégie DevOps et CI/CD
*Cette section justifie l'aspect "DevOps" du module.*

### 4.1. Gestion de Configuration et Versionning
* Utilisation de Git et du *Feature Branch Workflow*.
* Gestion des secrets et variables d'environnement (`.env`, `application.properties`).

### 4.2. Pipelines d'Intégration Continue (GitHub Actions)
Analyse des workflows mis en place :
* **Build :** Compilation automatique du Backend et du Frontend (`build.yml`).
* **Tests Automatisés :** Exécution des tests unitaires et d'intégration à chaque Push/PR (`test.yml`).
* **Analyse de Qualité :** Scan automatique via SonarCloud (`quality.yml`).
* **Rapport de Couverture :** Génération des rapports JaCoCo (`coverage.yml`).

### 4.3. Qualité du Code
* **SonarCloud :** Suivi de la dette technique, des code smells et de la duplication.
* **Linter :** Utilisation d'ESLint pour standardiser le code Frontend.

## 5. Tests et Validation
### 5.1. Stratégie de Test Backend
* **Tests Unitaires :** Isolation des services avec Mockito (ex: `UserServiceTest`, `TaskServiceImplTest`).
* **Tests d'Intégration :** Validation des flux complets avec base de données H2 (ex: `EventLocationIntegrationTest`).

### 5.2. Stratégie de Test Frontend
* Utilisation de **Vitest** pour tester les composants React et la logique UI.

## 6. Installation et Déploiement
* Prérequis techniques (Java JDK 21, Node.js 22).
* Procédure de lancement local (Gradle wrapper, npm run dev).
* Configuration des clés API (Google Maps).

## 7. Bilan et Perspectives
* **État actuel :** Résumé des fonctionnalités opérationnelles.
* **Limites :** Contraintes actuelles de l'algorithme d'optimisation.
* **Améliorations futures :**
    * Support du drag-and-drop avancé.
    * Prise en compte de la fatigue ou des préférences horaires de l'utilisateur.
    * Déploiement automatisé (CD) vers un cloud provider.

## 8. Conclusion
* Synthèse des apprentissages techniques (Spring Boot, React) et méthodologiques (DevOps, CI/CD).

---
**Annexes :**
* Diagramme de classes UML.
* Captures d'écran de l'application.