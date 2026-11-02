# Phase 5 — Registre d'exploitation et d'amélioration

Ce registre est un modèle à tenir par l'équipe d'exploitation après l'ouverture acceptée du service. Ne pas y inscrire de secret, de jeton, de mot de passe ni de donnée personnelle. Stocker les pièces contenant des informations sensibles dans l'outil interne autorisé et ne référencer ici que leur identifiant.

## Référents et engagements

| Élément | Valeur / référence |
|---|---|
| Environnement et URL | À renseigner |
| Version actuellement exploitée | À renseigner |
| Responsable métier | À renseigner |
| Responsable d'exploitation | À renseigner |
| Canal de support et horaires | À renseigner |
| Procédure d'escalade | À renseigner |
| Objectif de disponibilité et période de mesure | À convenir |
| Fréquence et rétention des sauvegardes | Selon la politique de l'organisation |
| Date et résultat du dernier exercice de restauration | À renseigner |
| Prochaine revue des accès | À planifier |
| Prochaine revue des dépendances et correctifs | À planifier |

## Registre des incidents

| ID | Date | Sévérité / impact | Version | Prise en charge | Résolution | Cause / action de prévention | État |
|---|---|---|---|---|---|---|---|
| À créer | — | — | — | — | — | — | — |

Pour chaque incident, consigner les heures de détection, prise en charge et rétablissement, le périmètre touché, les actions réalisées et le décideur de clôture. Préserver les journaux utiles selon la politique de conservation de l'organisation.

## Registre des évolutions

| ID | Demande / problème | Valeur métier | Priorité | Responsable | Version cible | Décision / état |
|---|---|---|---|---|---|---|
| À créer | — | — | — | — | — | — |

Avant toute livraison, vérifier la compatibilité des migrations Flyway, la couverture des parcours concernés, les permissions par rôle et la mise à jour des guides. Pour une mise à jour, suivre les contrôles avant déploiement et le retour arrière décrits dans [le dossier de phase 4](phase_4_execution.md).

## Revue périodique

| Période | Disponibilité | Incidents | Sauvegardes / restauration | Indicateurs métier | Décisions / actions | Responsable et date |
|---|---|---|---|---|---|---|
| À renseigner | À mesurer | À renseigner | À renseigner | À examiner | À consigner | À renseigner |

## Liste de contrôle de prise en charge

- [ ] Réception de phase 4 acceptée et version de référence enregistrée.
- [ ] Responsables, canal de support et escalade publiés.
- [ ] Accès opérationnels vérifiés et procédure de révocation disponible.
- [ ] Alertes de disponibilité et d'échec de sauvegarde configurées par l'exploitation.
- [ ] Politique de sauvegarde et exercice de restauration planifié.
- [ ] Cibles des indicateurs et fréquence de revue convenues avec le métier.
- [ ] Première revue périodique planifiée.
