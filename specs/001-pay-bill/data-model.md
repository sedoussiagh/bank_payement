# Modèle de données — Payer une facture

Les identifiants et instants sont stables lors des rejeux. Les instants sont stockés en UTC. L'intention garde la version de facture et le montant confirmés, même si une consultation ultérieure retourne une autre version. L'[interface](contracts/http.md) transporte les montants sous forme de chaînes décimales en MAD. Java emploie `BigDecimal` validé avant conversion exacte en unités mineures. Les bases utilisent des entiers signés 64 bits. Rejeter un montant hors représentation et vérifier les calculs avant mutation. Les lectures de solde dérivent du journal et des réservations ou de valeurs maintenues dans la même transaction locale.

## Payment — PostgreSQL

| Donnée | Champs et contraintes |
| --- | --- |
| `payment_order` | `order_id` UUID PK, `client_id`, intention normalisée complète, `idempotency_key`, statut, motif métier, références de preuves, `created_at`, `updated_at`. Unicité `(client_id, idempotency_key)` sur ce type unique de commande. Conserver l'association aussi après échec. |
| `payment_saga` | `order_id` PK/FK, `next_action`, `next_attempt_at`, `attempt_count`, `version`, `lease_until`, `reconciling_since`, `alert_due_at`, `alerted_at`. Index sur les échéances des ordres actifs. |
| `payment_receipt` | `receipt_id` PK, `order_id` unique, données stables du reçu. Créer dans la transaction qui publie `SUCCEEDED`. |

Insérer l'ordre et sa première action dans la même transaction. En cas de collision sur la clé, relire le gagnant après résolution du conflit, comparer l'intention normalisée et renvoyer le même ordre ou un conflit. La valeur canonique met à égalité `10.0` et `10.00`. Comparer toutes les données d'intention, sans dépendre d'un hash seul. Le rejeu précède toute revalidation mutable pour une clé déjà liée, après authentification du client.

## Account — PostgreSQL

| Donnée | Champs et contraintes |
| --- | --- |
| `account` | `account_id` PK, `owner_id`, statut, devise, solde comptable exprimé en unités mineures. Le solde initial est issu d'un mouvement d'ouverture. |
| `fund_hold` | `order_id` PK, `account_id` FK, montant, devise, état `ACTIVE`, `CONSUMED` ou `RELEASED`. Index de lecture des réservations actives par compte. |
| `account_action` | PK `(order_id, action)`, intention et résultat stable, y compris refus définitif de réservation. Une entrée avec intention différente constitue un conflit. |
| `journal_entry` et `journal_line` | Mouvement et lignes immuables. Un seul mouvement de paiement par ordre. Chaque mouvement possède des lignes dont le total signé est zéro dans la même devise. Une correction référence le mouvement original et ajoute un mouvement équilibré. |

Sous `READ COMMITTED`, verrouiller la ligne du compte avec `SELECT FOR UPDATE` avant la lecture du disponible et toute mutation de ses réservations ou de son solde. Calculer le disponible dans une instruction distincte après acquisition du verrou. Relire le journal d'action sous verrou avant toute nouvelle décision. Protéger aussi l'identité d'ordre par ses contraintes uniques. Un conflit inter-comptes provoque le rollback des écritures éventuellement tentées, suivi d'une relecture du résultat gagnant. Si un mouvement met à jour plusieurs comptes, acquérir tous leurs verrous dans le même ordre global. Une comptabilisation sur réservation active insère le mouvement équilibré, met à jour les soldes concernés, consomme la réservation et enregistre le résultat dans la même transaction. Une libération marque la réservation active et enregistre son résultat dans une seule transaction. La consommation et la libération sont exclusives. Une erreur transitoire ne devient pas un refus métier.

## Biller — MongoDB

| Collection | Champs et contraintes |
| --- | --- |
| `billers` | Identifiant de créancier et type fictif. |
| `invoices` | Identité unique `(billerId, invoiceRef)`, version, montant, devise, état et `committedOrderId` facultatif. |
| `biller_decisions` | `_id = orderId`, intention de facture, résultat `PENDING`, `COMMITTED` ou `DECLINED`, motif, `decisionDueAt` pour une réponse différée et référence stable pour l'issue finale. Index des échéances en attente. |

Le traitement d'une décision différée retrouve l'ordre en attente et revalide la version et l'éligibilité. Pour un engagement, mettre à jour conditionnellement la facture encore payable et sans engagement, puis écrire la décision finale dans la même transaction MongoDB. Le verrouillage optimiste par conflit d'écriture sur la facture sérialise deux engagements concurrents. Après conflit, relire ou rejouer sur une nouvelle transaction. Un ordre perdant reçoit un refus durable. Les décisions finales sont immuables, et une nouvelle soumission du même ordre relit sa décision. Toute mutation de l'éligibilité ou du montant d'une facture change sa version, dans le même document. Aucun montant n'est comptabilisé à partir d'un résultat de commit ambigu.

## Reprise et réconciliation

1. Réserver un lot d'actions échues côté Payment par une courte transaction de prise en charge, par exemple `FOR UPDATE SKIP LOCKED`, puis enregistrer un bail et avancer la version. Valider la transaction avant l'appel réseau.
2. Exécuter l'action distante avec l'identité stable, sans tenir de verrou PostgreSQL. Appliquer le résultat durable et programmer l'action suivante dans une transaction conditionnée par la version prise en charge et la validité du bail. Un travailleur dont le bail a expiré ne peut plus écrire le résultat. La reprise vérifie ou rejoue l'effet.
3. Rechercher l'effet déjà présent ou rejouer le même ordre après échéance de bail, délai réseau ou redémarrage. Un résultat absent après soumission déclenche une poursuite de la réconciliation avec la même identité. Après engagement, la seule action métier restante est la comptabilisation. Préserver le succès et son reçu pendant les rejeux.
4. Programmer les tentatives transitoires avec une pause initiale d'une seconde, doublée après chaque échec et plafonnée à 30 secondes. Valeurs proposées pour la démo, paramétrables. Conserver l'échéance d'alerte calculée à l'entrée en réconciliation. Le signalement utilise une échéance distincte des nouvelles tentatives, vérifie que l'ordre est toujours `RECONCILING` et renseigne `alerted_at` une seule fois. Le signalement ne bloque pas la suite. Rapporter les réponses contradictoires et consulter la preuve autoritative avant toute transition.
5. Journaliser les identifiants d'ordre et de l'action ainsi que les transitions, sans jetons ni données de compte détaillées. Une lecture opérations expose les dossiers signalés et leur état courant sans modifier la saga.
