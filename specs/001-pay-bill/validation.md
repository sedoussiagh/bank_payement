# Validation — Payer une facture

Cette matrice est la référence de couverture. Les critères détaillés figurent dans la [spécification](spec.md). Les tests du parcours métier restent à écrire.

| Règles | Critères | Niveau et preuve attendue |
| --- | --- | --- |
| FR-001, FR-002 | AC-01, AC-02 | Domaine : limites décimales et devise. Contrats/API : version obsolète, facture inconnue ou close, compte d'un autre client, contrôle d'accès au reçu. |
| FR-003 | AC-01, AC-03, AC-06 | Intégration PostgreSQL : soumissions synchronisées sur connexions distinctes, fonds à la limite, invariant des soldes et écritures équilibrées. |
| FR-004 | AC-04, AC-05 | Intégration PostgreSQL et API : compétition sur la clé, payload normalisé, conflit et isolation par identité. Après échec définitif, avancer l'horloge, redémarrer et vérifier que le rejeu retrouve le même ordre en échec sans nouvel effet. Une nouvelle clé peut créer un nouvel ordre si les préconditions sont réunies. |
| FR-005 | AC-01, AC-09 | Contrat et intégration MongoDB : compétition pour une facture, version revalidée après décision différée, refus rejouable et décision durable identique après reprise. Interrompre une transaction entre la modification de facture et la fin de la décision, puis vérifier l'absence d'engagement partiel. |
| FR-006 | AC-01, AC-07, AC-10 | Bout en bout sous Compose : vérifier la réservation avant soumission, l'engagement avant débit et l'absence de reçu avant confirmation des deux effets. Après engagement, injecter une panne avant comptabilisation puis une perte de réponse après comptabilisation. Vérifier zéro libération, une seule consommation de la réservation, un seul débit et un reçu stable. |
| FR-007 | AC-06, AC-07, AC-10 | Bout en bout : maintenir le résultat inconnu, contrôler l'horloge juste avant et au seuil de cinq minutes depuis l'entrée en réconciliation, puis répéter avec un seuil différent. Vérifier l'apparition du signalement au seuil, la poursuite des vérifications automatiques et la réservation toujours active. Injecter ensuite une acceptation durable avec réponse perdue ou un refus durable tardif et vérifier l'issue correspondante. |
| FR-008 | AC-04, AC-08, AC-09, AC-10 | Domaine : transitions invalides et réponses tardives. Intégration : arrêt après création, réservation, soumission, débit et libération, avant observation de chaque réponse. Rejouer et compter les effets. |
| FR-009 | AC-11 | Intégration du journal : historique préservé, écritures de correction équilibrées et référence au mouvement original. |
| FR-010 | AC-12 | Jalon outbox : injection d'échec avant validation de la transaction, puis arrêt après validation, rediffusion et déduplication à la consommation. |

Utiliser des données fictives réinitialisables, une horloge contrôlée et des points d'injection déterministes. En fin de tranche, publier les commandes réellement exécutées, les résultats par identifiant de critère et les limites observées. Avant la livraison du MVP, rejouer AC-01, AC-03 et AC-06 à AC-10 de bout en bout et rapprocher les résultats des SC-001 à SC-003. Le plan précisera l'emplacement des suites et les commandes reproductibles.

## Preuves techniques

- **T006 et T007** — Flyway 12.11.0 a appliqué V1 sur les deux bases PostgreSQL neuves. Les contraintes d'unicité Payment sont présentes. Un essai SQL annulé après vérification a confirmé l'équilibre du journal ainsi que le rejet d'une écriture déséquilibrée et d'une modification de ligne.
- **T008 et T009** — MongoDB a rejeté une facture dupliquée. Rejouer les index et les fixtures a conservé une seule facture de démonstration dans la pile de test. Le build Gradle a vérifié la signature et la séparation des identités client, opérations et service. Seule la pile de test publie des ports, tous sur l'interface de boucle locale.

- **T033** — Les deux suites PostgreSQL ont confirmé une requête sur la version 18.7. La suite MongoDB a confirmé le primaire 8.0.38, le commit d'une transaction et la conservation du document après réapplication du script d'index. Les trois tests passent avec Testcontainers 2.0.5 et Java 27 via le socket Linux de Rancher Desktop.
- **T034** — Les tests Payment et Account ont appliqué la V1 réelle sur une base PostgreSQL 18.7 vide, inséré une ligne représentative, puis appliqué une V2 réservée aux tests. Les données sont conservées et un rejeu applique zéro migration. Après altération de la copie temporaire de V1, Flyway signale `CHECKSUM_MISMATCH` et refuse de migrer.
- **T035** — Vérifier sur une pull request que la CI exécute le build et les intégrations, puis le paiement nominal, le rejeu et la reprise sous Compose. Confirmer qu'un test volontairement en échec fait échouer le workflow et que le rapport reste consultable.
