# Guide de validation prévu — Payer une facture

Le socle T001 à T005 se valide avec Gradle et Docker Compose. Les scénarios métier plus bas décrivent les vérifications à livrer avec le parcours de paiement.

## Préparer l'environnement

- Sélectionner le JDK 27 comme SDK du projet dans IntelliJ et comme JVM Gradle via `JAVA_HOME`. Le JDK local 27.0.1.1 répond par chemin direct et le PATH observé utilise Java 21. Vérifier `.\gradlew.bat --version`, `docker info` et `docker compose version`.
- Depuis la racine, exécuter `.\gradlew.bat build` pour produire les trois applications. Vérifier `docker compose config --quiet`, puis lancer `docker compose up --build -d --wait`. Compose monte des volumes séparés et attend le primaire MongoDB avant de lancer Biller.
- Vérifier les endpoints locaux `http://127.0.0.1:18081/actuator/health`, `http://127.0.0.1:18082/actuator/health` et `http://127.0.0.1:18083/q/health/ready`. Vérifier la transaction réelle avec le test Quarkus ci-dessous. Le test ouvre une collection temporaire et la supprime après vérification.
- Terminer avec `docker compose down` pour arrêter les services en conservant leurs volumes.

```powershell
$env:QUARKUS_MONGODB_CONNECTION_STRING = 'mongodb://127.0.0.1:27018/?replicaSet=biller-rs&directConnection=true'
.\gradlew.bat :services:biller-service:mongoTransactionTest
```

## Scénarios à livrer

| Scénario | Manipulation et observation |
| --- | --- |
| Nominal, AC-01 | Charger une facture de 140,25 MAD et un compte de 200,00 MAD sans autre réserve. Consulter la facture, confirmer et suivre `Location` jusqu'à `SUCCEEDED`. Lire le reçu puis vérifier les deux soldes à 59,75 MAD et le mouvement équilibré. |
| Concurrence, AC-03 et AC-09 | Synchroniser deux soumissions sur le même compte et sur des factures distinctes, puis deux ordres sur la même facture avec assez de fonds. Vérifier respectivement un disponible non négatif et un seul engagement. |
| Rejeu, AC-04 et AC-05 | Rejouer avant réception, après échec définitif et après un redémarrage. Soumettre une intention différente avec la même clé et une nouvelle tentative avec une nouvelle clé. Contrôler ordre stable, conflit puis création distincte quand admissible. |
| Refus et incertitude, AC-06 et AC-07 | Retarder la décision, avancer l'horloge jusqu'au seuil, vérifier le signalement sans libération, puis injecter un refus final ou une acceptation tardive. Vérifier la libération ou la comptabilisation et le statut final. |
| Reprise, AC-08 et AC-10 | Interrompre après la mutation d'un participant mais avant la persistance de sa réponse par Payment. Redémarrer et attendre la reprise. Vérifier la cardinalité des engagements, mouvements, libérations et reçus. Tester aussi la panne avant débit après engagement. |

Les appels à émettre et leurs codes figurent dans les [contrats](contracts/http.md). Une fois le sous-projet d'essai livré, lancer `.\gradlew.bat :tests:end-to-end:test` après le démarrage de Compose. La [matrice](validation.md) couvre aussi les preuves d'intégration et le jalon outbox. La suite doit produire des résultats identifiés par critère et des traces corrélées par ordre.
