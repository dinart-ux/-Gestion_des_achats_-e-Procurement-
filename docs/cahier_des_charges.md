# Cahier des charges

**Stage de fin d'études — 3 mois**  
**Intitulé :** Conception et développement d'une plateforme de gestion des achats (e-Procurement)

## 1. Contexte et justification

La gestion des achats (demandes d'achat, sélection des fournisseurs, bons de commande, réception et suivi budgétaire) est souvent gérée de manière dispersée dans les entreprises : échanges par email, tableurs et validations informelles. Cela entraîne des délais, un manque de traçabilité et des difficultés de contrôle budgétaire.

Le thème e-Procurement combine des enjeux fonctionnels riches (workflow de validation, gestion fournisseurs, suivi budgétaire) et des enjeux techniques (règles métier paramétrables, reporting, sécurité des accès).

## 2. Objectifs du stage

- Concevoir une plateforme de création de demandes d'achat en ligne.
- Mettre en place un circuit de validation hiérarchique selon le montant et le service.
- Gérer les fournisseurs et comparer les offres.
- Générer et suivre les bons de commande jusqu'à la réception.
- Suivre les budgets par service ou centre de coût et alerter en cas de dépassement.

## 3. Périmètre fonctionnel

### 3.1 Demandeur

Création de demandes d'achat d'articles ou de services (quantité, justification, budget estimé), suivi de leur statut et historique des demandes et commandes.

### 3.2 Validation

Liste filtrable des demandes à valider (service, montant, demandeur), validation, rejet ou demande de précision avec commentaire, et circuit à plusieurs niveaux selon le montant.

### 3.3 Fournisseurs et commandes

Référentiel fournisseurs et catégories, comparaison des offres, génération du bon de commande après validation, suivi des réceptions partielles ou totales.

### 3.4 Administration et budget

Gestion des utilisateurs, rôles et accès (demandeur, manager, service achats, administrateur), paramétrage des circuits, seuils et centres de coût, suivi budgétaire et alertes.

### 3.5 Reporting

Tableau de bord par période, service, fournisseur et catégorie, exports CSV/Excel, délai moyen de traitement, taux de rejet et dépenses par fournisseur.

## 4. Spécifications techniques

- Backend Spring Boot : API REST sécurisée, Spring Security et rôles.
- Frontend React responsive, sur ordinateur et mobile.
- Base PostgreSQL relationnelle.
- Authentification JWT.
- API découplée et organisée en couches contrôleur, service et dépôt de données.

## 5. Livrables attendus

- Document de conception : données, architecture et maquettes.
- Code source versionné (backend et frontend).
- Application déployée en environnement de test.
- Documentation technique et guide utilisateur.
- Support de soutenance.

## 6. Planning indicatif

- **Mois 1 :** analyse, modélisation des données, architecture et maquettes.
- **Mois 2 :** modules Demandeur et Validation, fournisseurs et commandes.
- **Mois 3 :** reporting, budget, tests, déploiement et documentation.

## 7. Critères d'évaluation

- Qualité de l'analyse fonctionnelle et de la conception technique.
- Fonctionnalité et robustesse de l'application.
- Respect des délais.
- Qualité de la documentation et de la présentation finale.
