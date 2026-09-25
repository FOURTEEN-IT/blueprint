---
name: neues-projekt
description: Wenn ein neues Projekt aus diesem Blueprint entstehen soll — fragt interaktiv ab, welche Technologie-Module (backend-java-onion, frontend-react-vite, persistence-postgres-flyway, deployment-fly, quality-gates) gebraucht werden, und spielt sie generalisiert in ein Zielverzeichnis ein.
---

# Neues Projekt aus dem Blueprint

Dieser Skill nimmt keine Fachentscheidung ab — er verdrahtet nur die
gewählten Technik-Module in ein neues (oder leeres) Zielprojekt. Was das
Projekt fachlich tut, bleibt außerhalb dieses Skills.

## Vorbedingung

Dieses Repo (`blueprint`) liegt lokal vor, mit `modules/` und
`docs/module-katalog.md`. Das Zielprojekt ist ein anderes, meist leeres
Repository/Verzeichnis.

## Schritte

1. **Modulauswahl abfragen.** Für jedes Modul in `docs/module-katalog.md`
   einzeln fragen (oder, falls der Nutzer die Auswahl schon im Prompt
   mitgegeben hat, daraus übernehmen):
   - `backend-java-onion` — praktisch immer ja, wenn irgendein anderes
     Modul gewählt wird (siehe Abhängigkeiten im Katalog).
   - `frontend-react-vite` — nur falls das Projekt eine eigene Oberfläche
     braucht (kein reines API-/Batch-Backend).
   - `persistence-postgres-flyway` — nur falls dauerhafte Persistenz
     gebraucht wird.
   - `deployment-fly` — nur falls Fly.io tatsächlich das
     Zielhosting ist. Bei Unsicherheit: `AskUserQuestion`, nicht raten —
     das Modul trifft eine Architekturannahme (genau eine Instanz), die
     nicht zu jedem Projekt passt (siehe README des Moduls).
   - `quality-gates` — nur sinnvoll mit `backend-java-onion`; empfehlen,
     aber nicht erzwingen.
   - `dependabot-maintenance` — nur wenn das Projekt auf GitHub liegt und
     eine Claude-Code-Routine einrichten kann/soll (die eigentliche
     Merge-Routine läuft außerhalb des Repos, siehe README des Moduls);
     empfehlen, aber nicht erzwingen.

   Ausgewählte Module gegen die Abhängigkeitsspalte in
   `docs/module-katalog.md` prüfen (z. B. `frontend-react-vite` ohne
   `backend-java-onion` ist nur eingeschränkt sinnvoll — dann nachfragen,
   ob das Projekt ein anderes Gradle-Projekt mitbringt).

2. **Platzhalter abfragen**, einmal projektweit, nicht je Modul einzeln
   wiederholen (jedes README listet, welche Platzhalter es tatsächlich
   verwendet — nicht jedes Modul braucht jeden):
   - `{{PACKAGE_BASE}}` (z. B. `de.example.projectname`)
   - `{{PROJECT_NAME}}`
   - `{{JAVA_VERSION}}` (nur bei `backend-java-onion`)
   - weitere modul-spezifische Platzhalter erst abfragen, wenn das
     jeweilige Modul tatsächlich gewählt wurde (siehe README des Moduls
     für die vollständige Liste — nicht hier duplizieren).

3. **Reihenfolge einhalten** (siehe `docs/module-katalog.md`, Abschnitt
   „Empfohlene Reihenfolge"): `backend-java-onion` zuerst, `quality-gates`
   direkt danach, dann `frontend-react-vite`/`persistence-postgres-flyway`
   in beliebiger Reihenfolge, `deployment-fly` danach,
   `dependabot-maintenance` ganz zuletzt.

4. **Je Modul einspielen:**
   - `template/` bzw. `src-template/`-Inhalte in die entsprechende Stelle
     im Zielprojekt kopieren (Pfade stehen im jeweiligen README).
   - `build.gradle.fragment.kts` in das `build.gradle.kts` des
     Zielprojekts zusammenführen (Plugins-Block, Dependencies-Block,
     Task-Definitionen an den passenden Stellen — nicht blind anhängen,
     Gradle-Blöcke gehören zusammen).
   - `.template`-Dateien (z. B. `ArchitectureTest.java.template`,
     `DatabaseConfig.java.template`, `fly.toml.template`) nach Ersetzen der
     Platzhalter ohne die Endung `.template` ablegen.
   - Alle `{{...}}`-Platzhalter im kopierten Inhalt durch die in Schritt 2
     gesammelten Werte ersetzen — keine Platzhalter im Zielprojekt stehen
     lassen.

5. **Modul-Wechselwirkungen beachten**, die kein Copy-Paste automatisch
   löst (aus den READMEs, hier nur als Erinnerung):
   - `persistence-postgres-flyway`: die Spring-Boot-Autoconfiguration für
     DataSource/Flyway muss in der Hauptanwendungsklasse des Zielprojekts
     ausgeschaltet werden (`exclude = [...]` in `@SpringBootApplication`).
   - `backend-java-onion`: `{{ADDITIONAL_ADAPTER_PORTS}}` in
     `ArchitectureTest.java.template` muss die tatsächlich vorhandenen
     Adapter-Unterpakete auflisten (entsteht erst, wenn klar ist, welche
     weiteren Module/Adapter das Projekt hat — diesen Schritt nach den
     anderen Modulen ausführen, nicht vorher).
   - `quality-gates`: referenziert Test-Tasks aus `backend-java-onion` —
     ohne dieses Modul lässt sich `quality-gates` nicht sinnvoll einspielen
     (Abbruch mit Hinweis, falls der Nutzer es trotzdem will).
   - `dependabot-maintenance`: Die Datei `docs/dependabot-routine.md` lässt
     sich einspielen und mit Platzhaltern befüllen wie jede andere Datei —
     die Routine selbst (der eigentliche „regelmäßige Blick auf offene
     PRs") entsteht dadurch aber noch nicht. Das ist ein manueller Schritt
     über die Claude-Code-Weboberfläche, den dieser Skill nicht automatisch
     auslösen kann; im Ergebnis-Hinweis (Schritt 7) explizit darauf
     hinweisen.

6. **Nach dem Einspielen: einmal bauen lassen.** Mindestens
   `./gradlew compileJava` (bzw. den entsprechenden Schnellcheck, falls
   kein Java-Modul gewählt wurde) — Platzhalter-Reste oder
   Zusammenführungsfehler zeigen sich meist schon hier.

7. **Ergebnis kurz zusammenfassen:** welche Module eingespielt wurden, was
   der Nutzer noch von Hand nachziehen muss (Secrets, Fly-App anlegen,
   Datenbank-Property setzen — je nach README der gewählten Module).

## Was dieser Skill nicht tut

Kein Feature-Prozess, keine ADR-Kette, keine Anforderungsdokumentation —
dieses Blueprint liefert nur Technik-Bausteine. Wie das Zielprojekt
danach arbeitet (Vorgehen bei neuen Features, Commit-Konventionen jenseits
des `commit-msg`-Hooks aus `quality-gates`, ADRs), entscheidet das Projekt
selbst.
