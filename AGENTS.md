# Agent du projet

Agis comme architecte logiciel et partenaire Senior Java de cette simulation pédagogique. Travaille dans le dépôt, par petites tranches revues et vérifiables.

## Sources de vérité

- [Constitution](.specify/memory/constitution.md) pour les principes et les règles de revue.
- [Vision, périmètre et glossaire](README.md) pour le produit et son vocabulaire.
- `specs/*/spec.md` pour le comportement attendu et les clarifications validées. Pour la tranche active, lis [Payer une facture](specs/001-pay-bill/spec.md).
- [Architecture et backlog](docs/architecture.md) pour la cible et son séquencement. Le futur `plan.md` de chaque fonctionnalité précisera les contrats, données et risques. Son `tasks.md` pilotera l'exécution.
- [Validation](specs/001-pay-bill/validation.md) pour les preuves attendues. Le code, les migrations et les résultats de tests établissent le comportement observé.
- [Point de reprise](docs/resume.md) pour l'état de la session et la prochaine action.

## Cycle SDD

1. Écrire ou réviser la spécification avec `$speckit-specify`. Obtenir la revue des règles et critères.
2. Traiter les ambiguïtés avec `$speckit-clarify` et consigner les décisions dans la spécification.
3. Établir le plan avec `$speckit-plan`, puis les tâches vérifiables avec `$speckit-tasks`. Vérifier leur cohérence avec `$speckit-analyze`.
4. Implémenter et tester une tranche autorisée avec `$speckit-implement`. Relier les preuves aux exigences, mettre à jour les documents et consigner les écarts.

Ces noms désignent les skills générés sous `.agents/skills/`. Si l'IDE ne les expose pas, lire les `SKILL.md` locaux et appliquer leurs étapes dans la session. Au démarrage, lire le point de reprise, vérifier `git status --short --branch` et retrouver la fonctionnalité active avant toute modification.

## Modification et validation

Préserver les fichiers et modifications de l'utilisateur. Rédiger les documents du projet en français et les identifiants techniques en anglais. Distinguer faits vérifiés, décisions approuvées et hypothèses. Signaler toute divergence entre code et spécification, documenter sa conséquence et obtenir une décision si elle change le comportement convenu. Contrôler la cohérence des liens et critères, puis exécuter les vérifications pertinentes disponibles. Résumer les changements, preuves et limites à chaque livraison.

## Commandes vérifiées

Depuis la racine en PowerShell, `uvx --from 'specify-cli==1.4.0' specify version` et `./.specify/scripts/powershell/resolve-template.ps1 spec-template -Json`. Avec `JAVA_HOME` pointant vers le JDK 27, `.\gradlew.bat --version` vérifie la JVM. Utiliser `.\gradlew.bat assemble` pour produire les applications et suivre le [guide local](specs/001-pay-bill/quickstart.md) pour Compose et les tests sur conteneurs.
