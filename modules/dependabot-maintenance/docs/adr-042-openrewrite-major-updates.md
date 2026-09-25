## ADR-0NN: Major-Versionsupdates über OpenRewrite-Rezepte statt gelesener Release Notes

**Status:** Akzeptiert

**Kontext:** Die tägliche Dependabot-Routine (siehe
`docs/dependabot-routine.md`) darf bei einem Major-Sprung auch
Produktivcode anfassen, um die PR grün zu bekommen. Ihr einziger
Anhaltspunkt dafür ist zunächst der PR-Text: Dependabot hängt die Release
Notes des angehobenen Pakets an, die Routine liest sie und schließt daraus,
was sich im Code ändern muss. Genau das ist die Stelle, an der ein
Sprachmodell rät — es leitet aus Prosa ab, was der Bibliotheksautor als
Umstieg gemeint hat, und ein Missverständnis fällt frühestens im nächsten
Gate auf, schlimmstenfalls gar nicht (eine Änderung, die kompiliert und
grün läuft, aber etwas anderes tut).

OpenRewrite dreht die Richtung um: Der Hersteller beschreibt den Umstieg
einmal als ausführbares Rezept (`UpgradeSpringBoot_4_0`,
`JUnit5to6Migration`, `Testcontainers2Migration` …), das auf dem
Syntaxbaum arbeitet statt auf Textersetzung. Was das Rezept ändert, ist
dann reproduzierbar und nachlesbar, nicht interpretiert.

**Entscheidung:**

1. **Das OpenRewrite-Gradle-Plugin gehört in `build.gradle.kts`, nicht in
   ein Init-Skript.** Ein Init-Skript würde den regulären Build gar nicht
   berühren, aber Dependabot pflegt nur, was es im Build sieht — das
   Werkzeug für Versionsupdates würde selbst veralten. Die Rezeptsammlungen
   liegen auf der eigenen Konfiguration `rewrite`, also weder auf dem
   Compile- noch auf dem Test-Classpath.

2. **Die Zuordnung Versionssprung → Rezept steht in einem eigenen Skript
   (`ci/openrewrite-anwenden.sh`), nicht im Build selbst.** Der Build weiß
   nicht, welches Rezept gerade gilt — das hängt am konkreten Sprung der
   jeweiligen Dependabot-PR und wird der Routine von außen (per
   `-PrewriteRezepte=...`) übergeben.

3. **Ohne explizit aktiviertes Rezept ändert `rewriteRun` nichts.** Ein
   versehentlicher Aufruf schreibt nicht um — das Aktivieren ist die
   Ausnahme, nicht der Normalfall.

4. **Der Katalog ist bewusst unvollständig und projektspezifisch zu
   pflegen.** Für Ökosysteme ohne vergleichbare Rezepte (z. B. npm,
   GitHub-Actions-Tags) bleibt es bei der Handarbeit der Routine — ein
   Sprung ohne Katalogeintrag ist eine Auskunft, kein Fehler.

**Konsequenzen:**

- Ein Rezept kennt die projektspezifischen Regeln nicht (Architekturregeln,
  Nullness, harte Invarianten) — der Rezeptlauf ist ein Vorschlag, über
  „grün" entscheiden weiterhin ausschließlich die Gates.
- Ein Rezept fasst nur an, was es als Muster kennt — hartkodierte Versionen
  (`resolutionStrategy`, `force`, explizite Pins), die eine alte
  Hauptversion erzwungen hat, wandern nicht automatisch mit und müssen nach
  einem Rezeptlauf gezielt durchgegangen werden.
- Springt das OpenRewrite-Plugin oder eine Rezeptsammlung selbst auf einen
  neuen Major, ist das Handarbeit — für das eigene Werkzeug gibt es kein
  Rezept.
