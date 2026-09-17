# dependabot-maintenance

Liefert die Abhängigkeitspflege als erzwungenen, wiederkehrenden Ablauf
statt als manuelle Erinnerung: Dependabot-Konfiguration (gebündelte
Minor/Patch-Updates, einzelne Major-Updates), CodeQL als eigenständige
Sicherheitsanalyse, OpenRewrite für reproduzierbare Major-Versionssprünge,
und die tägliche Routine, die offene Dependabot-PRs sichtet und mergt.

Kein Fachcode — nur das Wartungsgerüst.

## Was hier drinsteckt

| Datei/Ordner | Zweck |
|---|---|
| `template/.github/dependabot.yml.template` | Dependabot-Konfiguration: Minor/Patch je Ökosystem gebündelt, Major einzeln. |
| `template/.github/workflows/codeql.yml.template` | CodeQL-Workflow, unabhängig vom regulären Build-Workflow (läuft auch wenn dieser rot ist). |
| `template/ci/openrewrite-anwenden.sh.template` | Skript, das aus den Versionssprüngen einer PR das passende OpenRewrite-Rezept sucht und anwendet. |
| `build.gradle.fragment.kts` | OpenRewrite-Plugin + Rezeptsammlungen, `rewrite{}`-Block mit `-PrewriteRezepte`-Aktivierung. |
| `docs/dependabot-routine.md` | Die versionierte Quelle des Routine-Prompts — die eigentliche „Routine, die sich offene PRs regelmäßig anschaut". |
| `docs/adr-042-openrewrite-major-updates.md` | ADR-Vorlage: warum OpenRewrite-Rezepte statt gelesener Release Notes. |

## Abhängigkeit zu anderen Modulen

- **`backend-java-onion` (praktisch Voraussetzung):** OpenRewrite und die
  Dependabot-`gradle`-Konfiguration setzen ein Gradle/Java-Projekt voraus.
  Ohne dieses Modul entfällt `build.gradle.fragment.kts` und der
  `gradle`-Block in `dependabot.yml.template`.
- **`frontend-react-vite` (optional):** ergänzt einen eigenen
  `npm`-Dependabot-Block (auskommentiert in der Vorlage) und einen
  zweiten CodeQL-Sprachblock (`javascript-typescript`).
- **`quality-gates` (optional):** liefert die Gates, gegen die die Routine
  in Schritt 5c/5h prüft (`{{PRUEFEN_STUFEN}}` in `docs/dependabot-routine.md`).
- **`deployment-fly-cloudflare` (optional):** liefert die Semantic-Release-
  Konvention, gegen die die Routine ihre „niemals releasend committen"-Regel
  abgleicht (`{{RELEASING_COMMIT_TYPES}}`).

## Die Routine ist keine Gradle-/GitHub-Actions-Automatisierung

Anders als die übrigen Dateien dieses Moduls läuft `docs/dependabot-routine.md`
nicht im Repository selbst, sondern als **Claude-Code-Routine**
(`create_trigger`, täglich, frische Sitzung mit diesem Repo als Quelle) —
siehe die Einrichtungsanleitung in der Datei selbst. Das ist der Grund,
warum sie als Markdown-Dokument und nicht als `.github/workflows/*.yml`
vorliegt: Ihr Prompt wird über die Claude-Code-Weboberfläche eingetragen,
nicht von GitHub Actions ausgeführt.

## Katalog pflegen (`ci/openrewrite-anwenden.sh`)

Der Rezept-Katalog im Skript ist bewusst nur ein Beispiel (JUnit, ArchUnit,
Testcontainers) — kein vollständiger Satz. Ein Projekt ergänzt hier die
Bibliotheken, die es tatsächlich einsetzt, sobald ein Major-Sprung
tatsächlich anfällt (nicht auf Vorrat für Bibliotheken, die noch nie einen
Major-Sprung hatten). Rezeptnamen immer gegen die tatsächlich
veröffentlichte Rezeptsammlung prüfen (siehe Versionsangabe in
`build.gradle.fragment.kts`), nicht aus der Dokumentation abschreiben.

## Was ein Nutzer anpassen muss

- `{{MAIN_BRANCH}}`, `{{JAVA_VERSION}}`, `{{CODEQL_CRON}}`,
  `{{SKIP_FRONTEND_GRADLE_FLAG}}` — in den `.template`-Dateien unter
  `template/`.
- Alle Platzhalter in `docs/dependabot-routine.md` — siehe die Liste dort,
  Abschnitt „Platzhalter in diesem Prompt".
- Sprachmatrix in `codeql.yml.template` — Einträge entfernen, die nicht zu
  den gewählten Modulen passen (siehe Kommentare in der Datei).

## Offene Anmerkung

Diese Vorlage geht von GitHub (Dependabot, GitHub Actions, CodeQL) und
Claude Code (die Routine selbst) aus. Ein Projekt auf einer anderen
Plattform (GitLab Dependency Scanning, Renovate, ein anderes CI-System)
braucht eine eigene Übersetzung — das Prinzip (gebündelte Minor/Patch-
Updates, einzelne Major-Updates mit Rezept-Unterstützung, ein
wiederkehrender Wartungslauf) bleibt, die konkreten Dateien nicht.
