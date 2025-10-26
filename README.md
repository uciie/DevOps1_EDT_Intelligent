# Smart Scheduler

[![License](https://img.shields.io/github/license/uciie/DevOps1_EDT_Intelligent)](./LICENSE)
[![Version](https://img.shields.io/github/v/tag/uciie/DevOps1_EDT_Intelligent)](https://github.com/uciie/DevOps1_EDT_Intelligent/releases)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uciie_DevOps1_EDT_Intelligent&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uciie_DevOps1_EDT_Intelligent)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=uciie_DevOps1_EDT_Intelligent&metric=coverage)](https://sonarcloud.io/summary/new_code?id=uciie_DevOps1_EDT_Intelligent)
[![Build](https://github.com/uciie/DevOps1_EDT_Intelligent/actions/workflows/build.yml/badge.svg)](https://github.com/uciie/DevOps1_EDT_Intelligent/actions/workflows/build.yml)
[![Tests & SonarCloud](https://github.com/uciie/DevOps1_EDT_Intelligent/actions/workflows/test.yml/badge.svg)](https://github.com/uciie/DevOps1_EDT_Intelligent/actions/workflows/test.yml)

> **Emploi du temps intelligent** — import ICS, gestion d'utilisateurs et optimisation automatique des créneaux pour insérer des tâches.

---

## 📘 Sommaire
- [Smart Scheduler](#smart-scheduler)
  - [📘 Sommaire](#-sommaire)
  - [🌍 Aperçu](#-aperçu)
  - [🏗️ Architecture](#️-architecture)
  - [⚙️ Prérequis](#️-prérequis)
  - [🧩 Installation et configuration](#-installation-et-configuration)
    - [1️⃣ Cloner le projet](#1️⃣-cloner-le-projet)
    - [2️⃣ Ajouter le fichier `.env` dans `DevOps1_EDT_Intelligent/backend`](#2️⃣-ajouter-le-fichier-env-dans-devops1_edt_intelligentbackend)
  - [🚀 Lancer le backend](#-lancer-le-backend)
    - [En développement :](#en-développement-)
  - [💻 Lancer le frontend](#-lancer-le-frontend)
  - [🧠 API (extrait)](#-api-extrait)
  - [👥 Équipe](#-équipe)

---

## 🌍 Aperçu

**Smart Scheduler** est une application full-stack qui :
- **importe** des calendriers au format `.ics` (via `BiweeklyCalendarParser`),
- **optimise automatiquement** un emploi du temps en insérant des tâches dans les créneaux libres,
- **gère** les utilisateurs et leurs activités planifiées.

Cette solution propose un moteur d’optimisation extensible (stratégies de sélection configurables) et un backend Java moderne basé sur **Spring Boot**.

---

## 🏗️ Architecture

**Backend :**
- Spring Boot 3.x  
- Spring Web / Data JPA  
- PostgreSQL Cloud  
- Services métiers :  
  - `ScheduleOptimizerService`  
  - `UserService`  
  - `CalendarImportService`  

**Frontend :**
- React + Vite (Node.js / npm)
- Communication via API REST (`http://localhost:8080/api/...`)

**Parsing ICS :**
- Librairie [`biweekly`](https://github.com/mangstadt/biweekly)

**Diagramme UML :**
- Voir `uml/diagram_classes.puml` (PlantUML)

---

## ⚙️ Prérequis

| Outil | Version minimale | Description |
|--------|------------------|--------------|
| **Java** | 21 | Requis pour Spring Boot 3.x |
| **Gradle** | 9.0.0 | Outil de build |
| **Node.js** | 18+ | Pour le frontend |
| **npm** | Dernière stable | Gestionnaire de paquets Node |
| **PostgreSQL Cloud** | — | Base hébergée (Neon, Supabase, Railway, etc.) |

---

## 🧩 Installation et configuration

### 1️⃣ Cloner le projet
```bash
git clone <votre-repo> DevOps1_EDT_Intelligent
cd DevOps1_EDT_Intelligent
```

### 2️⃣ Ajouter le fichier `.env` dans `DevOps1_EDT_Intelligent/backend`
> ⚠️ **Ne pas committer** le fichier .env (il contient des informations sensibles et doit rester privé)
```bash
# fichier .env
DB_URL=jdbc:postgresql://ep-autumn-wind-abwdkup1-pooler.eu-west-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require
DB_USER=neondb_owner
DB_PASSWORD=npg_5Vx0NjUkQYun
```
---

## 🚀 Lancer le backend

### En développement :
La commande classique pour démarrer l’application est :
```bash
cd backend
./gradlew bootRun
```
ou sous Windows :
```bash
cd backend
gradlew.bat bootRun
```

Le backend est accessible sur :  
👉 [http://localhost:8080](http://localhost:8080)

---

## 💻 Lancer le frontend

Le dépôt ne contient pas encore de frontend complet, mais vous pouvez créer une démo React en quelques commandes :

```bash
cd frontend
npm run dev
```
L’interface sera accessible sur [http://localhost:5173](http://localhost:5173)

---

## 🧠 API (extrait)

| Méthode | Endpoint | Description |
|----------|-----------|-------------|
| `POST` | `/import` | Upload d’un fichier ICS (`multipart/form-data`, champ `file`) |
| `POST` | `/api/schedule/reshuffle/{eventId}` | Optimise le planning autour d’un événement |
| `POST` | `/api/users/register` | Crée un utilisateur |
| `GET` | `/api/users` | Liste les utilisateurs |

---

## 👥 Équipe

| Membre | GitHub | Num Étudiant |
|---------|---------|----|
| Paul Beyssac | [@BPaulz3trei](https://github.com/BPaulz3trei) | 42006035 |
| Manda Dabo | [@MandaDABO](https://github.com/MandaDABO) | 42012949 |
| Sylvain Huang | [@Kusanagies](https://github.com/Kusanagies) | 41005688 |
| Lucie Pan | [@uciie](https://github.com/uciie) | 45004162 |

---


> Projet académique open-source — *Smart Scheduler* © 2025



