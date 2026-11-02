# e-Procurement

Plateforme de gestion des demandes d'achat, validations, fournisseurs, commandes et budgets.

## Structure

- `backend/` : API Spring Boot et migrations Flyway.
- `frontend/` : application React + Vite.
- `docs/` : cahier des charges, schéma SQL et documentation du projet.

## Prérequis

- Java 17 ou plus (JDK).
- Node.js et npm.
- PostgreSQL installé et démarré sur le port `5435`.

## Créer la base PostgreSQL

Si `e-procurement` n'existe pas, créez-la dans pgAdmin : **Databases > Create > Database**, puis entrez `e-procurement` comme nom.

Vous pouvez aussi lancer cette commande depuis un terminal :

```powershell
psql -U postgres -h localhost -p 5435 -c 'CREATE DATABASE "e-procurement";'
```

Cette commande crée uniquement la base vide ; elle ne crée pas les tables.

**Important :** Flyway n'est pas opérationnel dans la configuration actuelle : ses dépendances Maven ne sont pas activées et `spring.flyway.enabled=false`. Hibernate utilise `ddl-auto=none`. Le backend ne crée donc ni ne met à jour le schéma au démarrage. Les migrations disponibles et le schéma SQL de référence présentent des divergences qui doivent être réconciliées avant toute initialisation automatique. Ne lancez pas les migrations sur une base existante sans sauvegarde, vérification de son schéma et validation de la stratégie.

## Configurer et démarrer le backend

Depuis `backend/`, copiez `.env.example` vers `.env`, puis remplacez `DB_PASSWORD` par le mot de passe de votre utilisateur PostgreSQL `postgres`. Remplacez aussi `JWT_SECRET` par une valeur aléatoire d'au moins 32 caractères. Ces valeurs restent locales et ne doivent pas être ajoutées au dépôt.

Les variables de connexion sont `DB_URL`, `DB_PASSWORD` et `JWT_SECRET`. Pour le nom d'utilisateur, `DB_USER` est prioritaire ; `DB_USERNAME` reste accepté pour les fichiers locaux déjà configurés. Les valeurs de l'exemple ciblent `localhost:5435/e-procurement` et l'utilisateur `postgres`.

Cette configuration suppose que le schéma nécessaire existe déjà dans la base. L'état et l'historique de la base locale doivent être vérifiés avant de décider comment appliquer les migrations.

```powershell
cd backend
Copy-Item .env.example .env
./mvnw.cmd spring-boot:run
```

L'API est disponible sur `http://localhost:8080` et Swagger UI sur `http://localhost:8080/swagger-ui.html`.

## Démarrer le frontend

Dans un autre terminal, depuis `frontend/`, copiez `.env.example` vers `.env`, puis lancez :

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

L'interface est disponible sur `http://localhost:5173`.

## Sources fonctionnelles et données

- Cahier des charges : [docs/cahier_des_charges.md](docs/cahier_des_charges.md)
- Schéma PostgreSQL fourni : [docs/e_procurement.sql](docs/e_procurement.sql)
- Migrations Flyway : `backend/src/main/resources/db/migration/`
- Comptes fictifs de développement : [docs/COMPTES_TEST.md](docs/COMPTES_TEST.md)

Pour ajouter les données de démonstration à la base locale, depuis `backend/`, exécutez le script protégé par environnement :

```powershell
psql -h localhost -p 5435 -U postgres -d e-procurement -v ON_ERROR_STOP=1 `
  -c "SET app.seed_environment='development'" `
  -f db/seed/development-demo.sql
```

Ce script ne fait pas partie des migrations et ne doit pas être exécuté en production. Il ne modifie ni ne supprime les données préexistantes.
