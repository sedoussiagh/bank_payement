# Recherche technique — Payer une facture

Les décisions ci-dessous sont des choix de conception proposés à la revue. Les références ont été vérifiées pour la compatibilité documentaire. Les essais locaux de la pile complète viendront avec le premier build.

## Java, frameworks et build

**Décision** — Java 27 en JVM demandé par le propriétaire, Gradle Wrapper 9.9.1 avec Kotlin DSL et un build multi-projet, Spring Boot 4.2.0 et Quarkus 3.40.1.2 LTS, dernières versions stables vérifiées au 19 novembre 2026. Utiliser des BOM distincts et verrouiller les versions des plugins Gradle et des plateformes par service.

**Motif** — Java 27 est une version publiée hors LTS. Les exécutables locaux `java` et `javac` 27.0.1.1 répondent par chemin direct. Gradle prend en charge Java 27 pour son daemon et ses toolchains depuis 9.8.0. La version stable 9.9.1 est retenue. Spring Boot 4.2.0 documente Java 27 et Gradle 9.x. Quarkus 3.40 documente Gradle et Kotlin DSL, mais son exemple recommande Gradle 9.7.0. L'ensemble Gradle 9.9.1, Java 27, plugin Quarkus, Quarkus REST, validation et pilote MongoDB reste à confirmer dans T005. La présence des deux versions de framework sur Maven Central a été vérifiée. [Calendrier Java](https://ops.java/releases/), [Spring Boot 4.2](https://docs.spring.io/spring-boot/4.2/system-requirements.html), [Publications Quarkus](https://quarkus.io/releases/), [Quarkus 3.40 et JDK 27](https://quarkus.io/version/3.40/guides/tls-registry-reference/), [Guide Gradle de Quarkus](https://quarkus.io/version/3.40/guides/gradle-tooling/), [Compatibilité Gradle](https://docs.gradle.org/9.8.0/release-notes.html#support-for-java-27), [Versions Gradle](https://gradle.org/releases/)

**Alternatives étudiées** — Java 25 est la LTS la plus récente. Tout changement de cible en cas d'incompatibilité constatée sera soumis au propriétaire. La DSL Groovy est également prise en charge. La DSL Kotlin est retenue pour expliciter la configuration du build et bénéficier de la complétion dans l'IDE.

## PostgreSQL, réservation et idempotence

**Décision** — PostgreSQL 18.7 comme référence et pgJDBC géré par le BOM Spring Boot 4.2.0. Verrou pessimiste de la ligne compte avant évaluation du disponible. Pour une nouvelle clé de confirmation, laisser la contrainte unique arbitrer la course, puis relire le gagnant dans une requête séparée si l'insertion rencontre un conflit.

**Motif** — Deux transactions locales ne doivent pas décider à partir du même disponible. Un verrou de ligne maintenu jusqu'au commit sérialise les mutations du même compte. Une seconde requête sous `READ COMMITTED` après le conflit permet d'observer le gagnant. Les références documentent la version, le pilote et la sémantique de verrouillage. [Versions PostgreSQL](https://www.postgresql.org/support/versioning/), [Versions gérées par Spring Boot](https://docs.spring.io/spring-boot/4.2/appendix/dependency-versions/coordinates.html), [Compatibilité pgJDBC](https://jdbc.postgresql.org/download/), [Verrous de ligne](https://www.postgresql.org/docs/18/explicit-locking.html#LOCKING-ROWS), [Isolation PostgreSQL](https://www.postgresql.org/docs/18/transaction-iso.html#XACT-READ-COMMITTED)

**Alternatives étudiées** — Le contrôle optimiste ou l'isolation `SERIALIZABLE` ajouteraient des reprises sur conflit. Le verrou par compte est explicite et permet une preuve de concurrence ciblée. Il suppose que tous les chemins de mutation respectent la discipline de verrouillage.

## MongoDB et engagement

**Décision** — MongoDB 8.0 sur replica set mono-nœud pour la démonstration. Le correctif d'image, son digest et l'essai Java 27 avec le pilote 5.6.5 référencé par le BOM Quarkus 3.40.1.2 sont à confirmer. Utiliser une transaction courte avec session explicite, mise à jour conditionnelle de la facture et persistance de la décision. Lire les décisions sur le primaire avec `readConcern: majority` et valider les transactions avec `writeConcern: majority`.

**Motif** — La documentation MongoDB décrit le support serveur de la lignée du pilote et les transactions sur replica set. Le format sépare une facture de ses tentatives sans perdre l'atomicité de l'engagement. Une simple lecture de facture suivie d'une écriture sur une décision séparée laisserait une course. La modification conditionnelle porte sur la facture et inclut sa version, son montant et son éligibilité. Les opérations du corps de transaction partagent la même session, sans effet externe. Après un commit ambigu, rechercher ou rejouer le même ordre. Le mono-nœud de démo permet ces tests sans redondance. [BOM Quarkus](https://github.com/quarkusio/quarkus/blob/3.40.1.2/bom/application/pom.xml), [Compatibilité du pilote](https://www.mongodb.com/docs/drivers/java/sync/current/reference/upgrade/#driver-version-5-5-server-support-changes), [Transactions MongoDB](https://www.mongodb.com/docs/v8.0/core/transactions-production-consideration/), [Transactions avec le pilote Java](https://www.mongodb.com/docs/drivers/java/sync/current/crud/transactions/), [Majorité](https://www.mongodb.com/docs/manual/reference/write-concern/#read-and-write-concern-recommendations), [Replica set de test](https://java.testcontainers.org/modules/databases/mongodb/#general-info)

**Alternatives étudiées** — Regrouper facture et décisions dans un document rendrait l'écriture atomique mais ferait grandir la facture avec ses tentatives. Un serveur MongoDB standalone n'offre pas cette transaction multi-document.

## Saga et transport

**Décision** — HTTP synchrone pour les échanges unitaires, réponse asynchrone à la confirmation client et exécution de la saga par un travailleur reprenable piloté par PostgreSQL. Le délai d'alerte de Q-02 s'applique indépendamment de la cadence de reprise. Utiliser les jetons de démonstration pour les contrats au premier jalon, avec Keycloak au jalon prévu.

**Motif** — Une panne après effet participant exige une réponse consultable ou un rejeu sûr. Un bail et une version coordonnent les travailleurs, tandis que les contraintes d'unicité des participants protègent les effets même en cas d'envois concurrents. Le client peut consulter son ordre dès son enregistrement.

**Alternatives étudiées** — Attendre tous les appels avant de répondre à la confirmation expose le client aux délais des participants. Un bus dès le premier jalon ajoute de l'infrastructure sans supprimer le besoin d'idempotence et de reprise. Le jalon outbox conserve sa preuve AC-12.

## Migrations et vérification continue

**Décision** — Utiliser Flyway pour chaque base PostgreSQL. Garder les scripts d'index Biller versionnés et rejouables, avec des fixtures séparées. Ajouter Testcontainers aux tests locaux et GitHub Actions aux pull requests.

**Motif** — Le plan repose sur des migrations SQL déjà nommées selon la convention Flyway. Flyway permet de contrôler l'historique et les checksums des scripts appliqués. Les tests sur les moteurs réels vérifient migrations, contraintes et transactions. La CI rejoue les commandes et conserve les preuves de validation. Les versions résolues des dépendances de test et de migration sont à confirmer au build avec les BOM retenues. [Spring Boot](https://docs.spring.io/spring-boot/4.2/how-to/data-initialization.html), [Flyway](https://documentation.red-gate.com/flyway/reference/commands/validate), [Testcontainers MongoDB](https://java.testcontainers.org/modules/databases/mongodb/), [CI Gradle](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle)

**Alternatives étudiées** — Liquibase est compatible avec Spring Boot. Flyway permet de conserver le découpage SQL prévu. Un historique dédié aux migrations de documents MongoDB sera ajouté dès qu'une évolution transforme des documents existants.
