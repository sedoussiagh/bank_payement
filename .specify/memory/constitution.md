<!--
Rapport de synchronisation pour la revue initiale
Gabarit natif vers proposition 1.0.0. Cinq principes et deux sections de projet renseignés.
Les gabarits natifs restent génériques. Ratification en attente de la revue du propriétaire.
-->
# Constitution de la simulation de paiement de factures

## Principes fondamentaux

### I. Simulation indépendante

Utiliser exclusivement des comptes, identités et créanciers fictifs. Toute démonstration présente le projet comme une simulation pédagogique indépendante, sans affiliation à une banque ou connexion à ses systèmes. Le portfolio décrit les preuves produites et ne revendique aucune expérience professionnelle bancaire.

### II. Frontières métier explicites

Chaque service métier possède ses règles et sa base. Aucun service ni BFF ne lit ou écrit la base d'un autre service. Les domaines restent indépendants des frameworks. Les commandes et requêtes sont séparées dans le code selon CQRS. Les états courants sont persistés directement, sans event sourcing.

### III. Invariants financiers vérifiables

Les montants portent une devise et une précision explicites. Les décisions financières reposent sur les données transactionnelles du service propriétaire. Toute écriture comptable est équilibrée et une correction ajoute des écritures traçables. Les tests doivent prouver la prévention du découvert par concurrence.

### IV. Reprise et effets distribués

Payment Service orchestre une saga persistée. Les participants exécutent des transactions ACID locales. Les effets rejouables sont idempotents et les résultats externes incertains sont réconciliés. Toute publication outbox est atomique avec la mutation locale correspondante. Les contrats admettent les doublons et la réorganisation des messages. Aucune garantie ACID globale ou exactly-once de bout en bout.

### V. Technologies justifiées par une preuve

Chaque ajout sert un scénario démontrable sur le poste local et apporte un critère de validation. Les caches restent hors des décisions financières. WebFlux requiert un chemin réellement non bloquant. La CDC brute est étudiée à part des contrats d'événements métier.

## Contraintes du projet

Le monorepo contient trois services métier et les BFF dédiés aux trois espaces utilisateurs. Docker Compose précède Kubernetes. Toute version figée exige une source officielle et une vérification de compatibilité. Toute version incertaine porte la mention « à confirmer ».

## Développement et revue

Passer de la spécification aux clarifications, au plan, aux tâches, à l'implémentation puis aux vérifications. Chaque tranche relie règles, critères, tests pertinents et résultat de démonstration. Vérifier les invariants avec des intégrations réelles aux bases concernées et reproduire les scénarios de panne déterminants. Consigner les écarts identifiés dans la revue.

## Gouvernance

La constitution guide la revue des spécifications et des changements. Une révision propose un motif, les artefacts affectés et les mesures de mise en conformité, puis requiert l'accord du propriétaire. Incrémenter la version majeure pour une rupture, mineure pour un principe ajouté ou substantiellement étendu, corrective pour une clarification sans changement normatif.

**Version proposée** 1.0.0 | **Ratification** en attente | **Dernière modification** 2026-11-19
