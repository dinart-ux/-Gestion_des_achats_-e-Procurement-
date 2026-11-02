# Phase 3 — Budget, reporting et préparation de livraison

## État de clôture

Les fonctionnalités et leur documentation sont présentes. La clôture reste conditionnée à la recette PostgreSQL sur une base dédiée, à la recette de déploiement sur l'environnement convenu et à l'approbation métier des indicateurs. Ces validations ne sont pas attestées dans le dépôt. Le taux de rejet d'une période sans demande est renvoyé à `0`.

Cette phase reprend le périmètre du troisième mois du cahier des charges : suivi budgétaire, indicateurs de pilotage, vérification, déploiement de test et documentation.

## Livré dans cette itération

- Consultation des budgets annuels par centre de coût. Les montants engagés sont calculés à partir des bons de commande émis pendant l'année concernée.
- Paramétrage du montant alloué et du seuil d'alerte par un administrateur.
- Alerte de seuil calculée dès que la consommation atteint le pourcentage défini. Le budget n'est pas bloquant.
- Indicateurs de reporting sur une période : nombre et taux de rejet des demandes créées dans la période, délai moyen soumission/commande pour les commandes émises, dépenses des bons émis pendant la période, et ventilation par service, fournisseur et catégorie.
- Écrans de budget, reporting et administration budgétaire. Les rapports sont accessibles au service achats et à l'administration ; la consultation des budgets par un manager est limitée à son service.
- Export CSV UTF-8 des indicateurs et regroupements affichés dans le reporting.

## Définitions des indicateurs

- **Taux de rejet** : nombre de demandes créées entre `du` et `au` dont le statut courant est `REJETEE`, divisé par le nombre total de demandes créées dans cette période. Le résultat est exprimé en pourcentage ; il est nul si aucune demande n'a été créée.
- **Délai moyen** : moyenne en jours calendaires entre la soumission d'une demande et l'émission de son bon de commande, pour les bons émis entre `du` et `au`. Les demandes sans date de soumission sont exclues ; le calcul porte sur les bons émis, indépendamment de la date de création de leur demande.
- **Dépenses** : somme des montants totaux des bons de commande émis entre `du` et `au`, sans déduction des réceptions ou annulations ultérieures.

Ces règles traduisent le cahier des charges et sont désormais appliquées par l'API et reprises dans le guide. Le métier peut les faire évoluer après recette.

## API

- `GET /api/v2/budgets?annee=2026` : budgets et engagements de l'année (année courante par défaut).
- `PUT /api/v2/budgets` : créer ou mettre à jour un budget (administrateur).
- `GET /api/v2/rapports?du=2026-01-01&au=2026-12-31` : agrégats de reporting (service achats ou administrateur).

## Vérification PostgreSQL

Le test `Phase3PostgresIntegrationTest` est prévu pour vérifier les migrations Flyway, les calculs de rejet, délai et dépenses, la consommation budgétaire et les permissions. Il est ignoré si `PG_INTEGRATION_URL` n'est pas défini. Son SQL de fixture cible encore `utilisateur` au singulier, alors que la migration V1 crée `utilisateurs` ; il doit être corrigé avant d'être considéré comme une preuve de fonctionnement.

Dans PowerShell, définir `PG_INTEGRATION_URL`, `PG_INTEGRATION_USERNAME` et `PG_INTEGRATION_PASSWORD` pour cette base dédiée, puis lancer depuis `backend/` :

```powershell
./mvnw.cmd test
```

Le test PostgreSQL est ignoré si `PG_INTEGRATION_URL` n'est pas défini. Ne définissez ces variables que pour une base jetable dédiée ; ne lancez jamais ce test contre la base locale ou partagée. Les tests unitaires et le test de contexte H2 peuvent être exécutés séparément sans connexion PostgreSQL.

## Livraison et critères restant soumis à l'environnement

- Le guide utilisateur et la procédure d'exploitation sont disponibles dans `guide_utilisateur.md` et `guide_exploitation.md`.
- La recette PostgreSQL dépend d'une instance disponible et d'une base dédiée. Le déploiement Docker dépend de Docker Compose et d'identifiants fournis pour l'environnement ciblé ; il doit être consigné après exécution.
- La définition des indicateurs est explicite et cohérente avec le cahier des charges ; l'approbation formelle du métier reste un acte de recette.

La phase ne peut être déclarée entièrement clôturée qu'après exécution de la recette PostgreSQL et du déploiement sur l'environnement convenu, puis validation métier des indicateurs.
