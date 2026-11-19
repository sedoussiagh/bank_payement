# Spécification fonctionnelle — Payer une facture

**Fonctionnalité** `001-pay-bill`

**Création** 2026-11-19

**Statut** Clarifications métier validées

**Besoin** Payer intégralement une facture fictive en MAD, suivre son issue et obtenir un reçu après succès.

## Clarifications

### Session 2026-11-19

- Q: Dans quel ordre réserver les fonds, engager le créancier et comptabiliser (Q-01) ? → A: Confirmer la réservation avant la soumission, obtenir l'engagement durable et irrévocable du créancier, puis confirmer la comptabilisation avant de publier le succès et le reçu. Après engagement, reprendre la comptabilisation du même ordre sans libérer la réservation.
- Q: Comment traiter une issue externe durablement incertaine (Q-02) ? → A: Réconcilier automatiquement, garder la réservation active et signaler le dossier aux opérations après un délai paramétrable de cinq minutes par défaut pour la démonstration.
- Q: Combien de temps conserver le lien entre une clé d'idempotence et son ordre (Q-03) ? → A: Pendant toute la durée de vie des données du MVP, y compris après un échec définitif. Une nouvelle tentative après cet échec exige une nouvelle clé.

## Scénarios utilisateurs et tests

### P1 — Consulter, confirmer et recevoir le reçu

Le client consulte une facture, vérifie le montant en MAD et confirme depuis un compte simulé sous son contrôle. Le créancier simulé décide de l'acceptation. L'équipe opérations suit les dossiers incertains et leurs preuves.

Préconditions nominales : identité de test reconnue, compte actif et autorisé, facture ouverte avec version et montant connus, fonds disponibles suffisants. La confirmation porte sur l'intégralité du montant de la version affichée.

Parcours nominal : enregistrer l'ordre, réserver les fonds, soumettre l'ordre au créancier, constater son engagement durable, comptabiliser et consommer la réservation, puis rendre le succès et le reçu consultables. Le client peut retrouver l'ordre pendant ce parcours.

- **AC-01** — **Given** une facture ouverte de 140,25 MAD et 200,00 MAD disponibles, **When** le client confirme et que le créancier s'engage, **Then** l'ordre devient `SUCCEEDED`, les deux soldes valent 59,75 MAD en l'absence d'autres réservations, un seul mouvement équilibré existe et le reçu est consultable.
- **AC-02** — **Given** une facture inconnue, close, modifiée depuis la consultation, un compte non autorisé ou un montant invalide, **When** le client tente de payer, **Then** le système signale le motif approprié et aucun débit ni engagement n'intervient. Si le changement de facture survient après réservation, le refus durable entraîne sa libération.
- **AC-03** — **Given** 100,00 MAD disponibles, **When** deux ordres sur des factures distinctes tentent simultanément de réserver 70,00 MAD chacun, **Then** au plus un obtient la réservation et le disponible reste positif ou nul. Avec 70,00 MAD disponibles et un seul ordre du même montant, la réservation est possible.

### P1 — Rejouer une confirmation en toute sécurité

- **AC-04** — **Given** une confirmation déjà enregistrée, **When** la même clé et la même intention sont soumises plusieurs fois, y compris concurremment avant réponse ou longtemps après un échec définitif, **Then** chaque rejeu renvoie le même ordre et son statut courant, sans recréer d'effet. Une nouvelle tentative après cet échec utilise une nouvelle clé.
- **AC-05** — **Given** une clé déjà liée à un ordre, **When** le même client la présente avec une autre intention, **Then** le système indique un conflit et ne crée aucun ordre. Une autre identité dispose de sa propre portée et ne peut obtenir l'ordre initial par cette clé.

### P1 — Traiter le refus, l'incertitude et la reprise

- **AC-06** — **Given** une réservation active, **When** un refus définitif est constaté, **Then** l'ordre passe par la libération et atteint `FAILED` après confirmation de la libération, avec disponible restauré et sans reçu.
- **AC-07** — **Given** une soumission sans réponse fiable, **When** le délai d'attente de la réponse expire ou que la consultation retourne « inconnu », **Then** la réconciliation se poursuit automatiquement, la réservation reste active et aucun reçu n'est publié. Si l'issue reste inconnue au seuil configuré depuis l'entrée en réconciliation, le dossier est signalé aux opérations et la réservation reste active. Une acceptation durable tardive conduit à la comptabilisation, un refus durable tardif à la libération.
- **AC-08** — **Given** un arrêt après un effet local et avant réception ou persistance de sa réponse, **When** le système redémarre et rejoue le traitement, **Then** le même ordre reprend sans réservation, engagement, débit, libération ou reçu dupliqué.
- **AC-09** — **Given** deux ordres valides sur la même facture et des fonds suffisants, **When** le créancier décide, **Then** au plus un ordre obtient l'engagement, le perdant libère ses fonds et aucun message dupliqué, tardif ou contradictoire n'annule une issue durable déjà établie.
- **AC-10** — **Given** un engagement durable et une réponse de comptabilisation momentanément indisponible, **When** le système reprend, **Then** il vérifie ou rejoue la comptabilisation du même ordre. La réservation reste active si la comptabilisation n'a pas eu lieu et est consommée une seule fois par celle-ci. Le succès et le reçu deviennent disponibles après confirmation.
- **AC-11** — **Given** une correction autorisée d'un mouvement déjà comptabilisé, **When** le correctif est appliqué dans le journal, **Then** l'écriture d'origine est lisible et la correction équilibrée la référence. Le déclenchement utilisateur est hors périmètre.
- **AC-12** — **Given** une mutation qui doit émettre un événement métier via outbox, **When** la transaction locale échoue ou réussit, **Then** la mutation et son entrée outbox sont respectivement toutes deux absentes ou toutes deux durables. Ce critère s'applique au jalon outbox.

## Exigences

### Règles métier et contraintes de cohérence

- **FR-001** — Un client peut consulter une facture et confirmer sa version, son montant intégral et son compte autorisé. Le créancier revalide la version et l'éligibilité lors de sa décision.
- **FR-002** — Montant strictement positif, devise `MAD`, précision maximale de deux décimales et calcul décimal exact. Rejeter la précision excédentaire et les autres devises sans arrondir.
- **FR-003** — Disponible = comptable moins réservations actives. Réserver seulement si le disponible couvre le montant, y compris en concurrence. Réserver et libérer laissent le comptable inchangé. Comptabiliser débite et consomme la réservation atomiquement dans Account Service, avec une contrepartie simulée de même montant.
- **FR-004** — La clé de confirmation est portée par l'identité client stable et l'opération de confirmation. Liaison atomique avec l'ordre et l'intention normalisée (créancier, facture, version, compte, montant et devise). Même intention, même identifiant et statut courant. Payload différent, conflit. La liaison dure toute la vie des données du MVP, sans expiration automatique, y compris après échec définitif. Une nouvelle intention exige une nouvelle clé.
- **FR-005** — Le créancier produit un engagement durable, irrévocable dans ce parcours, ou un refus définitif associé à l'ordre. Il garantit au plus un engagement par facture, même entre deux ordres distincts, et la stabilité du résultat lors des rejeux du même ordre. Tant que la décision est différée, l'ordre peut rester en attente.
- **FR-006** — Le succès client exige à la fois l'engagement et la comptabilisation locale confirmée. Avant soumission, l'ordre doit disposer d'une réservation confirmée. Après un engagement, vérifier ou rejouer uniquement la comptabilisation du même ordre, sans libération de la réservation. Le reçu stable identifie l'ordre, la facture, le créancier, le montant, la devise et la date de succès. Seul le client habilité peut accéder à son reçu.
- **FR-007** — Un timeout, une indisponibilité ou une absence de résultat observé ne prouve aucun refus. Après une tentative de soumission, toute libération requiert une preuve durable excluant l'engagement, telle qu'un refus enregistré. La réconciliation rapproche automatiquement les résultats par identifiant d'ordre et rejoue avec le même identifiant si nécessaire. À partir de l'entrée en réconciliation, un délai paramétrable de cinq minutes par défaut pour la démonstration déclenche le signalement aux opérations si l'issue reste inconnue. Le signalement conserve la réservation active et la réconciliation se poursuit.
- **FR-008** — L'état et la prochaine action de la saga survivent au redémarrage. Réservation, engagement, comptabilisation et libération sont rejouables par identifiant d'ordre et action. Des réponses dupliquées, tardives ou obsolètes sont traitées sans effet répété ni régression. Les réponses contradictoires sont signalées et vérifiées contre la source durable avant toute nouvelle décision.
- **FR-009** — Les écritures historiques sont conservées. Une correction est une opération distincte, équilibrée, référencée et auditable.
- **FR-010** — Au jalon outbox, la mutation métier et son événement à publier partagent la même transaction locale. Les consommateurs tolèrent les doublons. Les garanties restent locales, sans transaction ACID globale ni exactly-once de bout en bout.

### États et transitions

| État de départ | Preuve ou action attendue | État suivant |
| --- | --- | --- |
| Création | Intention valide et ordre enregistré | `RESERVING` |
| `RESERVING` | Réservation durable confirmée | `AWAITING_BILLER` |
| `RESERVING` | Refus définitif de réservation confirmé | `FAILED` |
| `AWAITING_BILLER` | Décision différée ou réponse non concluante | `RECONCILING` |
| `AWAITING_BILLER`, `RECONCILING` | Engagement durable confirmé | `POSTING` |
| `AWAITING_BILLER`, `RECONCILING` | Refus définitif confirmé | `RELEASING` |
| `POSTING` | Comptabilisation confirmée et reçu rendu disponible avec le succès | `SUCCEEDED` |
| `RELEASING` | Libération confirmée | `FAILED` |

Une action locale incertaine reste dans son état d'attente et se vérifie ou se rejoue. Un résultat contradictoire déclenche un signalement. Les états terminaux restent stables. Le motif d'échec est consultable par le client, sans détails internes.

### Entités métier

Facture (créancier, référence, version, montant, éligibilité), ordre (identité, intention, état et suivi), réservation (ordre, compte, montant et état), décision créancier (ordre, facture et résultat stable), mouvement comptable (écritures équilibrées), reçu (preuve du succès).

## Critères de succès

- **SC-001** — Chaque scénario nominal de la suite démontre un engagement, un mouvement équilibré et un reçu pour le même ordre.
- **SC-002** — Chaque scénario de concurrence et de rejeu conserve un disponible non négatif, au plus un engagement par facture et au plus un débit par ordre.
- **SC-003** — Chaque scénario de panne injectée converge vers l'issue durable dès que les participants répondent de nouveau. Si l'issue externe reste indéterminée, le dossier reste visible et la réservation active.

## Hypothèses et hors périmètre

Identités, factures et comptes de test préchargés. Hors périmètre de cette fonctionnalité : paiements partiels, change, frais, annulation après confirmation, remboursement ou correction déclenchés par interface et rapprochement interbancaire réel. L'outbox s'applique au jalon dédié.
