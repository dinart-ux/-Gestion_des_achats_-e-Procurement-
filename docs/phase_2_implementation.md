# Phase 2 — Modules métier

La phase 2 du planning couvre les parcours demandeur et validation, le référentiel fournisseur, les offres, les commandes et leur réception.

## État de départ constaté

Le schéma de référence est la migration Flyway `V1__initial_schema.sql`. Il contient déjà les tables métier nécessaires (`demande_achat`, `ligne_demande`, `validation`, `fournisseur`, `offre`, `bon_commande`, `ligne_bon_commande`, `reception` et `ligne_reception`). Les contrôleurs et services présents dans le code utilisent encore les anciens modèles (`demande`, `demande_item`, `commande`, `commande_detail`, `produit`). Ces tables n’existent pas dans cette migration. Les endpoints actuels ne peuvent donc pas réaliser ces parcours sur une base créée par Flyway.

## Incréments de livraison

### 2.1 Demandeur

- Créer un brouillon avec centre de coût, objet, justification et lignes libres (article ou service).
- Recalculer côté serveur le montant estimé à partir des lignes.
- Autoriser la modification et la suppression uniquement en brouillon et par son créateur.
- Soumettre une demande non vide et enregistrer la transition dans `historique_statut`.
- Afficher à l’utilisateur ses demandes, statuts, lignes et historique.

### 2.2 Validation

- Résoudre le circuit actif applicable au service et au montant de la demande.
- Créer une décision en attente par niveau de validation.
- N’autoriser une décision qu’au rôle attendu et au niveau courant.
- Prendre en charge l’approbation, le rejet motivé et la demande de précision motivée.
- Ne créer le bon de commande qu’après approbation du dernier niveau et choix d’une offre par le service achats.

### 2.3 Fournisseurs et offres

- Gérer le répertoire et les catégories de fournisseurs pour le service achats.
- Enregistrer des offres rattachées à une demande et un fournisseur, avec montant, délai et conditions.
- Afficher une comparaison des offres et empêcher la sélection d’une offre appartenant à une autre demande.

### 2.4 Commandes et réception

- Générer un numéro de commande unique à partir d’une demande approuvée et de l’offre retenue.
- Copier les lignes de la demande dans le bon de commande avec les prix retenus.
- Enregistrer les réceptions par ligne, sans dépasser les quantités commandées.
- Mettre à jour l’état de réception partielle ou complète de manière transactionnelle.

## Critères d’acceptation

- Aucun endpoint métier ne sérialise directement une entité JPA avec des relations paresseuses.
- Les listes et détails sont filtrés par rôle et périmètre côté serveur.
- Les montants et statuts ne sont jamais acceptés comme source de vérité depuis le navigateur.
- Les opérations de soumission, décision, commande et réception sont transactionnelles et inscrivent leur historique.
- La couche de persistance correspond au schéma Flyway ; aucun ajout de tables parallèles aux domaines déjà modélisés.

## Décision technique pour la reprise

Remplacer les endpoints basés sur les anciennes entités par des DTO et services qui ciblent le schéma Flyway. Conserver Flyway comme source de vérité et migrer les écrans React vers ces contrats API. Commencer par demande et validation, puis fournisseurs/offres, enfin commande/réception.

## État vérifié lors de la reprise de l'audit

Les assertions de livraison précédentes n'étaient pas étayées par les contrôles disponibles et ne doivent pas être prises comme preuve de complétude. Le code contient des parcours `/api/v2` pour les demandes et les offres, et les routes actives de modification/suppression d'un brouillon sont présentes. En revanche, les mappings de lecture des lignes de commande et d'enregistrement des réceptions appelés par React manquent encore. La configuration actuelle désactive Flyway et Hibernate ne crée pas le schéma. L'état de la base locale n'a pas pu être vérifié pendant cette reprise, car PostgreSQL ne répond pas sur le port 5435.

La phase 2 reste donc partielle. La compilation et les tests exécutés sont consignés dans `PLAN_CORRECTION_AUDIT.md` ; les parcours end-to-end sur une base PostgreSQL isolée restent à réaliser.
