# Phase 6 — Évolutions fonctionnelles et optimisation

## État

Phase cadrée ; les évolutions à réaliser sont à sélectionner à partir des bilans de phase 5 et à valider avec le responsable métier. Aucun backlog priorisé ni objectif de capacité n'est encore consigné dans le dépôt. Ce plan ne signifie pas que des évolutions ont été approuvées ou déployées.

La phase 6 transforme les besoins observés en exploitation en améliorations planifiées, mesurables et livrées de façon contrôlée. Elle couvre la priorisation métier, l'optimisation des parcours et des performances, ainsi que l'évolution maîtrisée des fonctions et données de la plateforme.

## Conditions d'entrée

- Bilan de phase 5 disponible : incidents, retours utilisateurs, indicateurs métier et contraintes d'exploitation.
- Responsable métier désigné pour arbitrer le périmètre et accepter les changements.
- Version de référence et environnement de recette disponibles.
- Procédure de livraison, sauvegarde et retour arrière de phase 4 applicable à la version en service.
- Besoins classés par valeur, urgence, risques et effort, avec critères d'acceptation convenus.

## Axes de travail

1. **Parcours métier** : analyser les étapes qui génèrent retards, erreurs ou ressaisies dans les demandes, validations, offres, commandes et réceptions.
2. **Performance** : établir une mesure de référence sur les API et écrans utilisés, puis optimiser les requêtes ou traitements qui dépassent les objectifs convenus.
3. **Qualité des données** : traiter les incohérences observées, améliorer les validations et définir les règles de correction sans altérer l'historique de traçabilité.
4. **Accessibilité et ergonomie** : recueillir les difficultés des utilisateurs et améliorer les parcours prioritaires sur ordinateur et mobile.
5. **Évolutions fonctionnelles** : livrer uniquement les demandes approuvées, avec migrations Flyway versionnées, permissions vérifiées et documentation mise à jour.
6. **Mesure des résultats** : comparer les indicateurs et retours avant/après, puis décider de poursuivre, corriger ou abandonner chaque changement.

## Déroulement d'une évolution

1. Enregistrer le besoin dans le registre de phase 5 et identifier le rôle utilisateur concerné.
2. Décrire le problème, le résultat attendu, les critères d'acceptation, les données concernées et les effets sur les permissions.
3. Faire approuver la priorité et le périmètre par le responsable métier ; évaluer les impacts techniques et d'exploitation.
4. Réaliser le changement sur une version identifiable et vérifier les parcours concernés dans l'environnement de recette.
5. Mettre à jour les guides, préparer la livraison et le retour arrière, puis obtenir l'acceptation métier.
6. Déployer selon la procédure de phase 4 et consigner la version, la décision et les mesures après livraison.

## Mesures de réussite

Les cibles sont définies avant chaque lot avec le responsable métier et l'exploitation. Elles peuvent inclure le temps de réponse des parcours ciblés, les erreurs ou reprises observées, le délai de traitement des demandes et l'adoption de la fonction livrée. Les indicateurs de reporting de phase 3 conservent leurs définitions validées ; toute modification de calcul nécessite une décision métier explicite et une documentation dédiée.

## Critères de sortie

- Chaque changement livré correspond à une demande approuvée et à des critères d'acceptation consignés.
- Les effets sur les rôles, données, migrations et exploitation sont évalués et vérifiés en recette.
- Les livraisons sont versionnées, documentées et accompagnées d'une procédure de retour arrière adaptée.
- Les résultats après mise en service sont comparés aux mesures de référence et les écarts ont un responsable.
- Le backlog restant est réévalué et transmis à la revue de phase 5.

## Références

- [Plan de phase 5](phase_5_plan.md)
- [Registre de phase 5](phase_5_execution.md)
- [Dossier de réception et retour arrière de phase 4](phase_4_execution.md)
- [Guide d'exploitation](guide_exploitation.md)
