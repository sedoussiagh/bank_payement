# Tâches — Payer une facture

Sources : [spécification](spec.md), [plan](plan.md), [modèle de données](data-model.md), [contrats](contracts/http.md), [validation](validation.md) et [guide d'essai](quickstart.md).

Les chemins sont relatifs à la racine. Dans les trois services, les frontières `domain`, `application` et `adapter` sont des packages Java. `[P]` signale des tâches indépendantes après leurs prérequis.

## Phase 1 — Mise en place

- [x] T001 Initialiser les sous-projets dans `settings.gradle.kts`, la toolchain Java 27 dans `build.gradle.kts` et le wrapper 9.9.1 avec le SHA-256 officiel de sa distribution dans `gradle/wrapper/gradle-wrapper.properties`. Vérifier aussi le SHA-256 publié de `gradle/wrapper/gradle-wrapper.jar` et l'exécution sous Java 27.
- [x] T002 [P] Initialiser les modules Spring Boot 4.2.0 et leurs BOM dans `services/payment-service/build.gradle.kts` et `services/account-service/build.gradle.kts`.
- [x] T003 [P] Initialiser le BOM et le plugin Quarkus 3.40.1.2, Quarkus REST et le pilote MongoDB synchrone dans `services/biller-service/build.gradle.kts`.
- [x] T004 [P] Définir les trois magasins à accès et volumes séparés dans `compose.yaml`. Initialiser le replica set MongoDB de façon idempotente. Conditionner le démarrage de Biller à la réussite de l'initialisation et à un primaire disponible. Vérifier le démarrage à vide et la persistance au redémarrage avec le volume existant.
- [x] T005 Construire et démarrer les modules JVM sous Java 27, avec leurs extensions, et vérifier la connexion PostgreSQL et une transaction MongoDB sur replica set. Après résolution réussie, versionner les `gradle.lockfile` des modules et vérifier leur résolution sans régénération. Consigner les versions et le résultat dans `specs/001-pay-bill/research.md`. Si l'essai échoue, isoler le composant incompatible avant de poursuivre le domaine.

## Phase 2 — Fondations

- [x] T006 [P] Configurer Flyway et son module PostgreSQL pour Payment dans `services/payment-service/build.gradle.kts` et préparer `services/payment-service/src/main/resources/db/migration/V1__payment.sql`. Créer l'ordre, la saga et le reçu avec unicité de l'ordre du reçu et de `(client_id, idempotency_key)`, ainsi que les échéances, la version et les baux.
- [x] T007 [P] Configurer Flyway et son module PostgreSQL pour Account dans `services/account-service/build.gradle.kts` et préparer `services/account-service/src/main/resources/db/migration/V1__account.sql`. Créer compte, réservation unique par ordre, action unique par ordre et action, journal immuable et mouvement de paiement unique par ordre.
- [x] T008 [P] Créer des scripts d'index versionnés et rejouables dans `services/biller-service/src/main/resources/mongo/`. Séparer les fixtures fictives. Vérifier l'identité unique `(billerId, invoiceRef)`, la décision par `_id = orderId` et l'index des décisions en attente.
- [ ] T009 Mettre en place les identités de démo et les frontières de test dans `compose.yaml` et `tests/end-to-end/build.gradle.kts`. Isoler les routes internes, fournir des identités de service et des jetons clients/opérations distincts, isoler les fixtures et activer JUnit Platform pour `:tests:end-to-end:test`.

## Phase 3 — US1 · Consulter, confirmer et recevoir le reçu · P1

**Essai indépendant** — Avec une facture de 140,25 MAD et un compte à 200,00 MAD, obtenir un reçu et un mouvement équilibré. Vérifier le rejet des intentions invalides et une course pour le disponible.

- [ ] T010 [P] [US1] Écrire les tests de contrat pour facture, solde, confirmation, suivi et reçu dans `tests/end-to-end/src/test/java/bankpayment/PaymentContractIT.java` à partir de `specs/001-pay-bill/contracts/openapi.yaml` (AC-01, AC-02). Vérifier rôles, propriété des ressources, clé d'idempotence et distinction entre absence de décision et refus durable côté Biller.
- [ ] T011 [P] [US1] Écrire la course simultanée de deux réservations et le cas du solde exact dans `services/account-service/src/test/java/bankpayment/account/ReservationConcurrencyIT.java` (AC-03).
- [ ] T012 [P] [US1] Implémenter les lectures de facture et le traitement nominal de la soumission dans `services/biller-service/src/main/java/bankpayment/biller/`. Enregistrer l'engagement et la décision dans une transaction MongoDB et revalider version, montant et éligibilité (FR-001, FR-005).
- [ ] T013 [P] [US1] Implémenter le solde et la réservation dans `services/account-service/src/main/java/bankpayment/account/`. Sérialiser par verrou de compte, enregistrer le refus durable et distinguer disponible et comptable (FR-003).
- [ ] T014 [US1] Implémenter la comptabilisation et la lecture de son résultat dans `services/account-service/src/main/java/bankpayment/account/`. Débiter, équilibrer, consommer la réservation et inscrire le résultat dans une seule transaction (FR-003, FR-006).
- [ ] T015 [US1] Implémenter confirmation et lecture d'ordre dans `services/payment-service/src/main/java/bankpayment/payment/`. Exiger MAD et un montant strictement positif à deux décimales maximum, valider le compte via Account, lier la clé à l'intention et atomiser ordre et première action (FR-001, FR-002, FR-004).
- [ ] T016 [US1] Enchaîner réservation, engagement et comptabilisation et créer le reçu avec `SUCCEEDED` dans `services/payment-service/src/main/java/bankpayment/payment/`. Vérifier l'ordre des preuves et l'accès réservé au propriétaire (FR-006).
- [ ] T017 [US1] Rejouer et documenter le parcours nominal et le cas des fonds insuffisants avec `tests/end-to-end/src/test/java/bankpayment/PaymentContractIT.java` et la suite Account (AC-01 à AC-03).

## Phase 4 — US2 · Rejouer une confirmation · P1

**Essai indépendant** — Soumettre la même intention concurremment, puis après échec et redémarrage. Vérifier le même ordre, le conflit sur intention différente et la séparation par identité.

- [ ] T018 [US2] Écrire les tests de compétition sur la clé, normalisation décimale, conflit et rejeu après échec dans `services/payment-service/src/test/java/bankpayment/payment/PaymentIdempotencyIT.java` (AC-04, AC-05).
- [ ] T019 [US2] Compléter l'arbitrage par contrainte unique, la relecture du gagnant et la comparaison de l'intention canonique dans `services/payment-service/src/main/java/bankpayment/payment/`. Conserver la clé pendant toute la vie des données. Renvoyer ordre et statut courant ou conflit selon le contrat (FR-004).
- [ ] T020 [US2] Vérifier sous Compose, dans `tests/end-to-end/src/test/java/bankpayment/PaymentReplayIT.java`, que le rejeu après échec et redémarrage conserve l'ordre et qu'une clé nouvelle permet une nouvelle tentative éligible (AC-04, AC-05).

## Phase 5 — US3 · Traiter le refus, l'incertitude et la reprise · P1

**Essai indépendant** — Provoquer un résultat inconnu, vérifier le signalement et la réservation, injecter une décision tardive puis redémarrer autour d'un effet durable.

- [ ] T021 [P] [US3] Écrire les tests de concurrence sur la facture, version modifiée et commit interrompu dans `services/biller-service/src/test/java/bankpayment/biller/BillerDecisionIT.java` (AC-02, AC-09).
- [ ] T022 [P] [US3] Écrire les scénarios de refus, réponse perdue, reprise et seuil d'alerte dans `tests/end-to-end/src/test/java/bankpayment/PaymentRecoveryIT.java` avec une horloge contrôlée (AC-06 à AC-10).
- [ ] T023 [P] [US3] Écrire les tests d'exclusion comptabilisation/libération et de correction équilibrée avec conservation de l'historique dans `services/account-service/src/test/java/bankpayment/account/AccountJournalIT.java` (AC-06, AC-08, AC-10, AC-11).
- [ ] T024 [P] [US3] Ajouter décisions différées `PENDING`, finalisation `COMMITTED` ou `DECLINED`, consultation par ordre et reprise à échéance dans `services/biller-service/src/main/java/bankpayment/biller/`. Revalider la facture lors de la finalisation et rejouer un commit indéterminé par identifiant (FR-005, FR-008).
- [ ] T025 [P] [US3] Ajouter la libération idempotente et la consultation des résultats d'action dans `services/account-service/src/main/java/bankpayment/account/`. Protéger les états `ACTIVE`, `CONSUMED` et `RELEASED` sous verrou et journaliser l'issue stable (FR-003, FR-008).
- [ ] T026 [US3] Implémenter le travailleur Payment dans `services/payment-service/src/main/java/bankpayment/payment/`. Prendre un bail et une version, exécuter les appels hors transaction, programmer le rejeu et ignorer les réponses obsolètes (FR-008).
- [ ] T027 [US3] Ajouter la réconciliation après résultat externe inconnu, la libération sur preuve de refus et la poursuite de la comptabilisation après engagement dans `services/payment-service/src/main/java/bankpayment/payment/` (FR-006 à FR-008).
- [ ] T028 [US3] Ajouter le signalement persistant et la lecture réservée aux opérations dans `services/payment-service/src/main/java/bankpayment/payment/`. Utiliser un seuil paramétrable de cinq minutes par défaut, calculé depuis l'entrée en réconciliation, sans arrêter les tentatives (FR-007).
- [ ] T029 [US3] Implémenter la correction interne par mouvement équilibré référencé dans `services/account-service/src/main/java/bankpayment/account/`. Vérifier que les écritures d'origine restent présentes et qu'aucune route publique de correction n'est créée (FR-009, AC-11).
- [ ] T030 [US3] Exécuter `tests/end-to-end/src/test/java/bankpayment/PaymentRecoveryIT.java` avec arrêts avant effet et après effet avant réponse. Vérifier l'unicité des effets, les états terminaux et la visibilité des dossiers incertains.

## Phase 6 — Vérification transversale

- [ ] T031 Consigner les commandes effectivement utilisées et le résultat par critère dans `specs/001-pay-bill/quickstart.md` et `specs/001-pay-bill/validation.md`. Vérifier SC-001 à SC-003 sur les preuves collectées.
- [ ] T032 Mettre à jour `docs/resume.md` et vérifier la trace de FR-010 et AC-12 vers le jalon J4 dans `docs/architecture.md`. Le cas outbox sera implémenté et vérifié dans la spécification de ce jalon.
- [ ] T033 Configurer Testcontainers dans les `build.gradle.kts` des sous-projets `services/payment-service/`, `services/account-service/` et `services/biller-service/`. Vérifier le démarrage des versions de base retenues, une requête PostgreSQL, une transaction sur le replica set MongoDB et le rejeu des scripts d'index sans modification des données. Réutiliser ces conteneurs dans les suites d'intégration.
- [ ] T034 Ajouter les tests de migrations Flyway dans `services/payment-service/src/test/java/bankpayment/payment/PaymentMigrationIT.java` et `services/account-service/src/test/java/bankpayment/account/AccountMigrationIT.java`. Vérifier l'installation à vide, la mise à niveau avec des données existantes et le refus après modification du checksum d'une migration appliquée. Si V1 est seule, utiliser une migration de test pour la mise à niveau.
- [ ] T035 Créer `.github/workflows/ci.yml` pour les pull requests. Exécuter le build Gradle sous Java 27 et les tests avec Testcontainers, puis lancer Compose et les scénarios bout en bout de paiement, rejeu et reprise. Publier les rapports et démontrer un succès et un échec détecté. Après le premier lancement sur une pull request, rendre le contrôle obligatoire sur la branche par défaut si les paramètres du dépôt le permettent et vérifier qu'une PR en échec ne peut pas être fusionnée.

## Dépendances et ordre d'exécution

- Après T001, T002, T003 et T004 sont indépendantes. T005 vient après ces trois tâches. T006 à T008 peuvent ensuite avancer en parallèle. T009 prépare l'environnement d'essai.
- T033 suit T005 et précède les suites d'intégration sur conteneurs. T034 suit T006, T007 et T033. Ces tâches ajoutées gardent leurs identifiants même si elles précèdent la validation finale.
- US1 après les fondations. T010 à T013 peuvent avancer en parallèle. T014 dépend de T013, T015 du contrat Account, et T016 de T012, T014 et T015. T017 valide la tranche.
- US2 après US1. T018 conduit T019, puis T020 prouve l'intégration du rejeu.
- US3 après US1 et US2 pour l'intégration. Les tests T021 à T023 et les adaptations participants T024 et T025 peuvent être répartis sur des fichiers distincts. T027 et T028 s'appuient sur T024 à T026. T029 suit T025 et la mise en place du journal. T030 exige les trois parcours.
- La vérification transversale suit les preuves des trois parcours.
- T035 suit T030, T033 et T034. T031 consigne également les preuves de T034 et T035.

## Stratégie d'implémentation

Livrer d'abord US1 comme tranche de démonstration J1. US2 puis US3 complètent les garanties du MVP avant de passer aux interfaces web. Chaque tranche associe la preuve d'intégration aux règles concernées et conserve les erreurs et scénarios reproductibles.
