# Contrats HTTP v1 — Payer une facture

Proposition pour revue. JSON UTF-8. Identifiants opaques. Les montants utilisent l'objet `{ amount, currency }`, avec une chaîne décimale et zéro à deux chiffres fractionnaires pour `amount` et `MAD` pour `currency`. Le montant d'un paiement doit être strictement positif. Les soldes d'un compte client peuvent être nuls. Une valeur équivalente est normalisée à deux décimales. Utiliser des instants RFC 3339. Les codes métier stables sont indépendants des messages affichés.

Le [contrat OpenAPI](openapi.yaml) formalise le premier parcours de paiement, avec la réservation, la comptabilisation et la consultation de la décision Biller. Les routes de libération, de lecture des actions Account et de consultation opérations restent décrites ci-dessous en attendant leur tranche.

## Interfaces de consultation et de confirmation

| Appel | Entrée | Résultat |
| --- | --- | --- |
| `GET /v1/billers/{billerId}/invoices/{invoiceRef}` (Biller) | Identité de test et références | `200` avec références, version, montant et éligibilité, ou `404 INVOICE_NOT_FOUND`. La consultation reste informative. |
| `GET /v1/accounts/{accountId}/balance` (Account) | Jeton du propriétaire | `200` avec `bookedBalance` et `availableBalance` sous forme d'objets montant. `404` si le compte est inconnu ou appartient à un autre client. |
| `POST /v1/payments` (Payment) | Jeton client, en-tête obligatoire `Idempotency-Key`, corps `billerId`, `invoiceRef`, `invoiceVersion`, `accountId`, `money` | `202` après enregistrement durable, avec `Location` et `orderId`, `status`. Rejeu identique `200` avec statut courant et même `Location`. `409 IDEMPOTENCY_CONFLICT` si la clé liée porte une autre intention. |
| `GET /v1/payments/{orderId}` (Payment) | Jeton du propriétaire | `200` avec état, montant, références et éventuel motif métier. |
| `GET /v1/payments/{orderId}/receipt` (Payment) | Jeton du propriétaire | `200` avec `receiptId`, `orderId`, références de facture et créancier, montant et `succeededAt`. Avant succès, `404 RECEIPT_NOT_AVAILABLE`. |
| `GET /v1/operations/reconciliation-alerts` (Payment) | Jeton opérations | `200` avec liste bornée et paginée par curseur stable. Chaque élément donne `orderId`, `reconcilingSince`, `alertedAt` et état courant. |

Pour un nouvel ordre, une requête mal formée ou un montant/devise invalide produit `400`, une identité absente ou invalide `401` et un rôle non habilité `403`. Sur la lecture d'un ordre ou reçu appartenant à un autre client, répondre `404` comme pour un ordre inexistant. Le service valide les identités et rôles par jeton signé. Il ne déduit pas le client d'un champ fourni dans le corps. Account vérifie la propriété du compte lors de la réservation, en utilisant l'identité portée par l'ordre transmis par Payment. Quand une règle métier est évaluée après création durable, l'état de l'ordre véhicule le refus, par exemple `ACCOUNT_NOT_AUTHORIZED`, `INSUFFICIENT_FUNDS` ou `INVOICE_VERSION_MISMATCH`. La clé est une chaîne opaque de 1 à 128 caractères ASCII visibles, sensible à la casse. Les échecs de validation avant création ne lient pas la clé.

## Interfaces internes

Les routes suivantes exigent une identité de service autorisée. Le service destinataire compare l'intention reçue à celle déjà persistée pour cet identifiant et répond `409 OPERATION_CONFLICT` en cas de divergence. Un tel conflit est un incident à examiner. Il ne constitue pas un refus financier.

| Appel | Intention | Résultat durable |
| --- | --- | --- |
| `PUT /internal/v1/account-orders/{orderId}/reservation` (Account) | `clientId`, `accountId`, `money` | `200 RESERVED` ou `200 DECLINED` avec motif stable. |
| `PUT /internal/v1/account-orders/{orderId}/posting` (Account) | Identité d'ordre, compte et montant qui correspondent à la réservation | `200 POSTED` et `postingId`. Un état antérieur incompatible produit `409 INVALID_RESERVATION_STATE`. |
| `PUT /internal/v1/account-orders/{orderId}/release` (Account) | Identité d'ordre, compte et motif de libération | `200 RELEASED`. Une réservation consommée produit `409 INVALID_RESERVATION_STATE`. |
| `GET /internal/v1/account-orders/{orderId}/actions/{action}` (Account) | Action parmi `reservation`, `posting`, `release` | `200` avec le résultat d'action stable, ou `404 ACTION_NOT_OBSERVED`. |
| `PUT /internal/v1/biller-orders/{orderId}` (Biller) | `billerId`, `invoiceRef`, `invoiceVersion`, `money` | `202 PENDING` ou `200 COMMITTED` / `200 DECLINED`, avec référence de décision finale et éventuel motif. Les rejeux lisent la décision courante du même ordre. |
| `GET /internal/v1/biller-orders/{orderId}` (Biller) | Identité d'ordre | `200` avec état `PENDING`, `COMMITTED` ou `DECLINED` et les mêmes preuves disponibles, ou `404 DECISION_NOT_OBSERVED`. |

`COMMITTED` et `DECLINED` de Biller désignent des décisions persistées. Après soumission, délai réseau, `5xx` et résultat non observé conduisent à réessayer ou à relire. Le payload stable d'une action et son identifiant restent les mêmes à chaque tentative. Payment peut vérifier le résultat d'une comptabilisation ou d'une libération déjà appliquée via la lecture d'action. Les réponses d'erreur HTTP hors résultats métier portent `code` et `correlationId`, sans détails internes. La validation d'entrée n'autorise aucune compensation automatique.
