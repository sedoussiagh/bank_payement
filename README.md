# Simulation de paiement de factures en MAD

Projet personnel et simulation pédagogique indépendante, sans affiliation à une banque réelle, sans connexion à un système bancaire réel et sans données personnelles réelles. Le projet documente un apprentissage et ne revendique aucune expérience professionnelle bancaire.

## Vision

Apprendre les technologies par des tranches exécutables, préparer des entretiens Senior Java, Tech Lead et Architecte, puis constituer un portfolio d'arbitrages argumentés. Une démonstration doit permettre de suivre un paiement nominal, d'observer une panne, puis de vérifier la reprise et les écritures produites.

## MVP

Dans un contexte marocain fictif, un client consulte une facture d'eau, d'électricité ou de télécoms, confirme son paiement intégral en MAD et suit son état. Le système réserve les fonds, traite une réponse immédiate ou différée, comptabilise ou libère selon l'issue, réconcilie les résultats incertains et fournit un reçu après succès. Les opérations peuvent consulter les dossiers bloqués. Le créancier simulé expose ses factures et les résultats selon des scénarios reproductibles.

Le premier parcours vertical peut utiliser des données préchargées et des interfaces de test. L'espace client Angular est le premier espace web proposé. Les espaces créancier et opérations React complètent la cible par étapes. Les paiements partiels, le change, les frais, le découvert, les intégrations bancaires réelles et les remboursements en libre-service sont hors MVP.

## Glossaire

| Terme | Sens |
| --- | --- |
| MAD | Dirham marocain, devise unique du MVP. |
| Facture | Dette fictive rattachée à un créancier, à un montant et à une version. |
| Ordre de paiement | Intention confirmée et suivie sous un identifiant stable. |
| Solde comptable | Solde issu des écritures comptabilisées. |
| Solde disponible | Part du solde comptable utilisable après déduction des réservations actives. |
| Réservation | Affectation de fonds à un ordre en attente de comptabilisation ou de libération. |
| Engagement externe | Acceptation durable et irrévocable simulée par le créancier pour un ordre donné. |
| Comptabilisation | Enregistrement équilibré de l'opération et consommation de sa réservation. |
| Saga | Enchaînement persistant de transactions locales et décisions de reprise. |
| Réconciliation | Recherche et rapprochement des résultats durables afin de poursuivre un ordre incertain. |
| Reçu | Preuve consultable après succès de l'ordre. |
| BFF | Service d'interface dédié à un espace utilisateur. |

Voir la [première spécification](specs/001-pay-bill/spec.md), la [cible et le backlog](docs/architecture.md) et le [point de reprise](docs/resume.md).
