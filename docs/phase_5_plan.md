# Phase 5 — Maintien en conditions opérationnelles et amélioration continue

## État

Phase cadrée, démarrage opérationnel à planifier après la réception de phase 4. Aucun environnement de production, responsable d'exploitation, canal de support ou indicateur de disponibilité n'est déclaré dans le dépôt ; ces éléments doivent être renseignés avant le démarrage du suivi. Ce document définit le dispositif à appliquer et ne constitue pas une preuve d'exploitation.

La phase 5 prend le relais après la mise en service acceptée. Elle vise à maintenir la disponibilité et la sécurité de la plateforme, traiter les incidents, suivre les usages et prioriser les évolutions sur la base de retours métier.

## Conditions d'entrée

- Réception de phase 4 documentée, version déployée identifiée et responsable métier ayant accepté l'ouverture du service.
- Responsable d'exploitation, canal de support et procédure d'escalade communiqués.
- Sauvegardes et restauration vérifiées dans l'environnement exploité.
- Accès aux journaux, alertes et métriques validés par l'exploitation, sans exposer de données personnelles ou de secrets.
- Référentiel des utilisateurs et procédure de révocation des accès attribués à des responsables.

## Activités

1. Surveiller la disponibilité des sondes, les erreurs applicatives, les migrations, l'espace disque et l'état des sauvegardes.
2. Enregistrer chaque incident avec son impact, ses horaires, la version touchée, les actions, la cause identifiée et la décision de clôture.
3. Qualifier les demandes de support et les retours utilisateurs ; distinguer incidents, demandes d'accès, questions et évolutions.
4. Examiner périodiquement les droits et comptes, les dépendances et les correctifs de sécurité disponibles ; planifier les mises à jour avec sauvegarde, vérification et retour arrière.
5. Analyser les indicateurs métier existants avec le responsable métier, puis ordonner les évolutions selon leur valeur, leur urgence et leur effort.
6. Publier une note de version et mettre à jour les guides après chaque évolution livrée.

## Indicateurs à convenir

Les cibles sont fixées avec le responsable métier et l'exploitation avant la période de suivi ; aucune valeur cible n'est présumée ici.

| Indicateur | Définition de suivi | Périodicité |
|---|---|---|
| Disponibilité | Temps où l'interface et l'API répondent correctement, rapporté au temps de la période | Mensuelle |
| Incidents | Nombre par sévérité, durée de prise en charge et durée de résolution | Mensuelle |
| Sauvegardes | Exécutions réussies et date du dernier exercice de restauration vérifié | À chaque sauvegarde / exercice planifié |
| Évolutions | Demandes reçues, priorisées, livrées et reportées | Mensuelle |
| Usage métier | Indicateurs de reporting de phase 3, selon les définitions validées en recette | Selon le rythme métier |

## Critères de sortie

- Les incidents et demandes sont suivis par un responsable et leur état est visible aux interlocuteurs concernés.
- Les sauvegardes font l'objet de contrôles réguliers et la procédure de restauration est exercée selon la politique de l'organisation.
- Les accès et mises à jour sont revus selon une fréquence convenue et les actions sont consignées.
- Un bilan périodique des indicateurs, incidents, risques et évolutions est partagé avec le responsable métier.
- Les guides et le registre des versions correspondent à la version réellement exploitée.

## Références

- [Dossier de réception de phase 4](phase_4_execution.md)
- [Procédure d'exploitation](guide_exploitation.md)
- [Guide utilisateur](guide_utilisateur.md)
