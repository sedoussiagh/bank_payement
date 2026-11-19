# Guide de validation prévu — Payer une facture

Ce guide décrit l'interface d'exécution à livrer pendant l'implémentation. Le dépôt contient uniquement le socle documentaire. Les commandes dépendant du futur code restent des commandes cibles.

## Préparer l'environnement

- Sélectionner le JDK 27 comme SDK du projet dans IntelliJ et comme JVM Gradle via `JAVA_HOME`. Le JDK local 27.0.1.1 répond par chemin direct et le PATH observé utilise Java 21. Vérifier `java -version`, puis `.\gradlew.bat --version` dès que le wrapper existe, ainsi que `docker info` et `docker compose version`.
- Depuis la racine, la configuration future de Compose fournit des volumes séparés, des contrôles de santé, des identités fictives et un replica set MongoDB initialisé avant le démarrage de Biller. Les identités de test et les scénarios de panne sont disponibles uniquement dans l'environnement de validation.
- Une fois le code en place, exécuter `.\gradlew.bat :services:payment-service:build :services:account-service:build :services:biller-service:build`, puis `docker compose config --quiet` et `docker compose up --build -d --wait`. Lancer la suite Compose avec `.\gradlew.bat :tests:end-to-end:test`. Terminer avec `docker compose down` pour conserver les données si une enquête est nécessaire. Le sous-projet d'essai doit isoler ses fixtures et fournir des instructions de remise à zéro dédiées.

## Scénarios à livrer

| Scénario | Manipulation et observation |
| --- | --- |
| Nominal, AC-01 | Charger une facture de 140,25 MAD et un compte de 200,00 MAD sans autre réserve. Consulter la facture, confirmer et suivre `Location` jusqu'à `SUCCEEDED`. Lire le reçu puis vérifier les deux soldes à 59,75 MAD et le mouvement équilibré. |
| Concurrence, AC-03 et AC-09 | Synchroniser deux soumissions sur le même compte et sur des factures distinctes, puis deux ordres sur la même facture avec assez de fonds. Vérifier respectivement un disponible non négatif et un seul engagement. |
| Rejeu, AC-04 et AC-05 | Rejouer avant réception, après échec définitif et après un redémarrage. Soumettre une intention différente avec la même clé et une nouvelle tentative avec une nouvelle clé. Contrôler ordre stable, conflit puis création distincte quand admissible. |
| Refus et incertitude, AC-06 et AC-07 | Retarder la décision, avancer l'horloge jusqu'au seuil, vérifier le signalement sans libération, puis injecter un refus final ou une acceptation tardive. Vérifier la libération ou la comptabilisation et le statut final. |
| Reprise, AC-08 et AC-10 | Interrompre après la mutation d'un participant mais avant la persistance de sa réponse par Payment. Redémarrer et attendre la reprise. Vérifier la cardinalité des engagements, mouvements, libérations et reçus. Tester aussi la panne avant débit après engagement. |

Les appels à émettre et leurs codes figurent dans les [contrats](contracts/http.md). La [matrice](validation.md) couvre aussi les preuves d'intégration et le jalon outbox. La suite doit produire des résultats identifiés par critère et des traces corrélées par ordre.
