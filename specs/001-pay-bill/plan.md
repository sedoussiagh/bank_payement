# Plan d'implémentation — Payer une facture

**Fonctionnalité Spec Kit** `001-pay-bill` · **Date** 2026-11-19 · **Statut** Tâches préparées après feu vert, pile révisée · [Spécification](spec.md) · [Tâches](tasks.md)

## Résumé

Livrer le parcours via trois services et trois bases isolées sous Docker Compose. Payment enregistre l'ordre et pilote sa progression durable. Account décide de la disponibilité et comptabilise dans une transaction locale. Biller décide de l'engagement sur la version de facture soumise. Après validation du parcours nominal et des invariants concurrents, démontrer les décisions différées, le signalement et la reprise après panne. Les interfaces web et l'outbox rejoignent leurs jalons propres décrits dans le [backlog](../../docs/architecture.md).

## Contexte technique

| Sujet | Choix proposé |
| --- | --- |
| Langage et build | Java 27 en mode JVM, Gradle Wrapper 9.9.1 avec Kotlin DSL et toolchain Java 27 et BOM propres à chaque framework. |
| Dépendances principales | Spring Boot 4.2.0 MVC, JDBC et migrations SQL pour Payment et Account. Quarkus 3.40.1.2 LTS avec Quarkus REST et client MongoDB synchrone côté Biller, sur des threads aptes aux accès bloquants. |
| Stockage | PostgreSQL dédié à chacun des deux services Spring. MongoDB en replica set à un nœud pour Biller dans Compose. Versions et limites dans la [recherche](research.md). |
| Validation | JUnit et Testcontainers avec PostgreSQL et MongoDB en replica set pour les intégrations. Tests de contrat HTTP et scénarios bout en bout sur Compose. GitHub Actions exécute les suites sur les pull requests. |
| Plateforme | Conteneurs Linux sous Rancher Desktop / WSL2. L'environnement Windows reste utilisable pour le build et les scripts d'essai. |
| Capacité | Jeu de données fictif et petit nombre de services sur poste local. Les tests imposent de vraies courses et vérifient le seuil d'alerte. Aucun objectif de débit ou SLA de production. |
| Sécurité | Identités fictives via jetons signés de démonstration, injectés par l'environnement de test. Vérifier signature, audience, expiration, rôle et propriété des ressources. Credentials internes distincts et routes internes confinées au réseau Compose. |

## Contrôle de la constitution

| Principe | Évaluation avant recherche et après conception |
| --- | --- |
| Simulation indépendante | Conforme. Fixtures et identités fictives. |
| Frontières métier | Conforme. Chaque base possède ses migrations et ses accès. Le domaine reste indépendant du framework. Séparer handlers de commande et de requête sans projection distribuée obligatoire. |
| Invariants financiers | Conforme au design. Verrou du compte et écritures équilibrées dans une transaction. Preuves sur PostgreSQL attendues. |
| Reprise et effets distribués | Conforme au design. État et échéances persistés, effets idempotents par participant. Aucun appel distant dans une transaction locale. Outbox atomique au jalon J4. |
| Technologies justifiées | Conforme. HTTP bloquant et trois bases couvrent la tranche. Les composants différés restent rattachés au backlog. |
| Versions et preuves | Support Java 27 documenté par Spring Boot. Compatibilité de la combinaison Quarkus, extensions et pilote sous Java 27 vérifiée au démarrage et par le test de transaction T005. |

## Structure du projet

### Documentation de la fonctionnalité

- [spec.md](spec.md) et [validation.md](validation.md) portent les règles et la traçabilité.
- [research.md](research.md) consigne les arbitrages et leurs sources.
- [data-model.md](data-model.md) définit les invariants de stockage et la reprise.
- [contracts/http.md](contracts/http.md) décrit les interfaces HTTP. Le [contrat OpenAPI](contracts/openapi.yaml) fournit les schémas vérifiables du premier parcours de paiement.
- [quickstart.md](quickstart.md) définit les vérifications reproductibles à livrer.

### Structure source proposée

```text
settings.gradle.kts
build.gradle.kts
gradle/wrapper/gradle-wrapper.properties
compose.yaml
services/
  payment-service/
  account-service/
  biller-service/
tests/
  end-to-end/
```

Chaque service est un sous-projet Gradle avec son propre `build.gradle.kts`, ses sources et ses tests, avec des packages `domain`, `application` et `adapter`. Les domaines restent indépendants et les services échangent via HTTP. Payment et Account appliquent leurs migrations SQL et maintiennent chacun leur historique Flyway. Biller exécute des scripts d'index versionnés et rejouables. Les fixtures de démonstration sont séparées des migrations. Le sous-projet `:tests:end-to-end` possède ses fixtures et s'exécute séparément après le démarrage de Compose.

## Déroulement

- Parcours vertical J1. Construire consultation, confirmation, réservation, engagement, comptabilisation et reçu. Vérifier les écritures et les courses de réservation sur la base réelle.
- Robustesse J2. Ajouter décisions différées, échéances de reprise, signalement et scénarios de panne. Le parcours reçoit ses preuves de conformité aux AC-01 à AC-11 sur l'ensemble J1–J2. Le test de correction AC-11 porte sur le journal, sans route de correction publique.
- J4. Ajouter la transactional outbox et la preuve AC-12 via une spécification d'évolution.

## Risques et parades

| Risque | Parade et preuve |
| --- | --- |
| Débit multiple après réponse perdue ou bail expiré | Unicité des effets côté participant et garde sur la version côté Payment. Pannes AC-08 et AC-10. |
| Engagement concurrent ou ambigu | Transaction MongoDB, concurrence sur le document facture, décision par identifiant d'ordre. Échec de confirmation de commit traité comme résultat indéterminé. Preuve AC-09. |
| Réservation bloquée | Reprise planifiée, signalement après seuil, consultations par ordre. Aucune libération sur la seule absence de preuve. |
| Environnement local et dépendances | Les versions du JDK, des BOM et des images ainsi que le replica set ont été vérifiés au premier build et au démarrage. |

Aucun écart à la constitution ne requiert de dérogation pour ce design.
