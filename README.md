# blueprint

Ein modularer Baukasten für neue Projekte: keine Fachlogik, sondern die
technischen Bausteine (Backend-Gerüst, Frontend-Gerüst, Persistenz,
Deployment, Qualitäts-Gates), die sich in der Referenzimplementierung
[Watchparty](https://github.com/ajFourteen/Watchparty) bereits bewährt
haben — hier herausgelöst, generalisiert und einzeln wählbar gemacht.

Kein Modul enthält Watchparty-Fachcode. Jedes Modul ist für sich
dokumentiert und über `{{...}}`-Platzhalter an ein neues Projekt anpassbar.

## Module

Siehe [`docs/module-katalog.md`](docs/module-katalog.md) für die
Übersicht, Abhängigkeiten und empfohlene Einspiel-Reihenfolge:

- `backend-java-onion` — Java/Spring-Boot in Onion-Architektur
- `frontend-react-vite` — React/Vite, Build ins Backend-Jar eingebettet
- `persistence-postgres-flyway` — Postgres über Flyway, ohne Spring Data
- `deployment-fly-cloudflare` — Fly.io-Hosting, GitHub Actions, Cloudflare optional
- `quality-gates` — Mutationstests, JGiven-Report, commit-msg-Hook

## Verwendung mit Claude Code

Dieses Repo ist so gebaut, dass Claude Code (bzw. Claude als Agent) es
direkt bedienen kann: `.claude/skills/neues-projekt/` ist ein Skill, der
die Modulauswahl abfragt und die gewählten Module in ein Zielprojekt
einspielt.

Um ein neues Projekt daraus zu erzeugen, diesen Prompt in Claude Code
kopieren (im Zielverzeichnis/-repo, mit diesem Blueprint-Repo als
zusätzlicher Quelle verfügbar) und nach Bedarf anpassen:

```text
Ich möchte ein neues Projekt auf Basis des Blueprint-Repos
(FOURTEEN-IT/blueprint) aufsetzen. Lies dort docs/module-katalog.md und
die READMEs der Module unter modules/, dann rufe den Skill
`neues-projekt` auf.

Gewünschte Module: <z. B. backend-java-onion, quality-gates,
persistence-postgres-flyway — oder "schlag mir passende Module vor,
basierend auf folgender Projektbeschreibung: ...">

Projektname: <...>
Basis-Package: <z. B. de.example.projectname>
Kurzbeschreibung des Projekts: <ein bis zwei Sätze, was das Projekt tut>

Frag mich alles Nötige, was in den Modul-READMEs als Platzhalter oder
offene Anmerkung steht, bevor du etwas einspielst.
```

Der Skill fragt daraufhin gezielt nach, was noch fehlt (Platzhalter,
Modul-Wechselwirkungen), hält sich an die empfohlene Einspiel-Reihenfolge
aus dem Modul-Katalog und fasst am Ende zusammen, was manuell noch zu tun
bleibt (Secrets, Datenbank-Property, Fly-App anlegen o. ä.).

## Was hier bewusst fehlt

Kein Feature-Prozess, keine ADR-Kette, kein Entscheidungsprotokoll — dieser
Blueprint liefert nur Technik, keine Arbeitsweise. Wie ein aus diesem
Blueprint entstandenes Projekt von der Idee bis zum Commit arbeitet,
entscheidet das Projekt selbst.
