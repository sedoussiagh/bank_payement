# Point de reprise

## État au 2026-11-19

Le dépôt a été trouvé sans commit ni code applicatif, avec des fichiers IntelliJ préexistants sous `.idea/`. Il contient le socle Spec Kit, la [spécification clarifiée](../specs/001-pay-bill/spec.md), la [matrice de validation](../specs/001-pay-bill/validation.md), le [plan technique révisé](../specs/001-pay-bill/plan.md) et ses [tâches](../specs/001-pay-bill/tasks.md). La [constitution](../.specify/memory/constitution.md) reste proposée.

## Faits vérifiés

- Windows 11 Entreprise, environ 31,3 Gio de RAM détectés. Distributions Rancher Desktop sous WSL2 en cours d'exécution. Moteur Docker 27.3.1 joignable et Docker Compose 2.39.2 disponible.
- Java sur le PATH : IBM Semeru 21.0.8. Les exécutables OpenJDK 27.0.1.1 ont été vérifiés dans le répertoire local `.jdks`. Java 27 et le Gradle Wrapper 9.9.1 sont les cibles retenues. Le premier essai de build et de démarrage est décrit dans les tâches.
- Spec Kit 1.4.0 a répondu via `uvx --from 'specify-cli==1.4.0' specify version`. Le téléchargement depuis GitHub par Git échoue à la résolution du proxy. La voie PyPI officielle a fonctionné. Le socle a été généré en répertoire temporaire, avec `uvx --from 'specify-cli==1.4.0' specify init --here --integration codex --script ps --non-interactive --ignore-agent-tools`, puis les seuls répertoires `.agents` et `.specify` ont été copiés dans le dépôt.
- L'intégration officielle vise Codex CLI. La découverte native des skills dans IntelliJ reste à vérifier. Les guides locaux restent lisibles depuis une session IDE. [Installation officielle](https://github.github.com/spec-kit/installation.html), [Intégration Codex CLI](https://github.github.com/spec-kit/integrations/codex.html)

## Décisions et revue attendue

Q-01 à Q-03 sont validées et consignées dans les [clarifications](../specs/001-pay-bill/spec.md#clarifications). Le plan propose des contrats HTTP, un verrou par compte, une transaction MongoDB pour l'engagement, et des baux persistés pour la reprise. Les versions de référence et les vérifications locales restantes figurent dans la [recherche technique](../specs/001-pay-bill/research.md).

## Prochaine action

Au début de l'implémentation, lire `AGENTS.md` et vérifier le statut Git. Si le pointeur local est absent, sélectionner `specs/001-pay-bill` via `SPECIFY_FEATURE_DIRECTORY` ou recréer `.specify/feature.json` avec `{"feature_directory":"specs/001-pay-bill"}`. Commencer par T001 à T005 avec `$speckit-implement`, puis utiliser le résultat de l'essai Java 27 pour confirmer la pile avant le code métier.
