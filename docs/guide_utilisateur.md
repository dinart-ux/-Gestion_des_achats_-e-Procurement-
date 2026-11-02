# Guide utilisateur — e-Procurement

## Connexion

Connectez-vous avec votre compte. Les menus disponibles dépendent de votre rôle : demandeur, manager, service achats ou administrateur. Les droits sont également contrôlés par l'API.

## Demandes d'achat

Dans **Demandes**, créez une demande en renseignant son objet, son centre de coût, sa justification et au moins une ligne. Le total estimé est calculé à partir des quantités et prix unitaires. Ouvrez le détail pour vérifier le dossier, le soumettre, consulter son statut et relire son historique. Un brouillon peut être modifié ou supprimé par son créateur.

## Validation

Les managers voient les demandes de leurs collaborateurs directs au niveau qui leur est attribué. Examinez le détail et choisissez **Valider**, **Rejeter** ou **Demander une précision**. Un commentaire est requis pour les deux dernières actions. Une demande de précision renvoie le dossier au demandeur.

## Fournisseurs, offres et commandes

Le service achats peut enregistrer un fournisseur, ajouter les offres reçues pour une demande validée, les comparer et retenir une offre. Après sélection, créez le bon de commande. Dans **Commandes**, renseignez les quantités reçues à chaque livraison ; elles ne peuvent pas dépasser les quantités commandées.

## Budgets et reporting

Les managers consultent les budgets de leur service. Le service achats et les administrateurs consultent les budgets de tous les centres actifs. Le montant engagé correspond aux bons de commande émis dans l'année. Une alerte s'affiche au seuil configuré ; elle n'empêche pas une commande. L'administrateur peut modifier l'enveloppe et son seuil dans **Budgets**.

Le tableau de bord du service achats et de l'administrateur présente les indicateurs de la période choisie. Le taux de rejet rapporte les demandes actuellement rejetées aux demandes créées pendant cette période. Le délai moyen est calculé entre la soumission et l'émission du bon, parmi les bons émis pendant cette période ; les demandes sans date de soumission ne sont pas comptées. Les dépenses sont la somme des montants des bons émis pendant la période, sans déduction des réceptions ou annulations ultérieures. Elles sont ventilées par service, fournisseur et catégorie. Choisissez une période et utilisez **Exporter en CSV** pour télécharger les résultats. Une période sans demande affiche un taux de rejet nul ; une période sans bon émis ne fournit pas de délai moyen.

## Démarrage de l'environnement local

Suivez les étapes de configuration et de démarrage du `README.md`. Dans la configuration actuelle, le backend n'exécute pas les migrations Flyway et ne crée pas le schéma au démarrage. La base doit avoir été initialisée selon une procédure validée par l'équipe technique. Configurez les secrets locaux dans les fichiers `.env` décrits par les exemples ; ne placez pas de secret d'environnement de test ou de production dans Git.
