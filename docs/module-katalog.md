# Modul-Katalog

Jedes Modul unter `modules/` ist eigenständig dokumentiert (eigenes
`README.md`). Diese Seite ist nur die Übersicht: was es liefert, wovon es
abhängt, und in welcher Reihenfolge Module sinnvoll eingespielt werden.

| Modul | Liefert | Voraussetzt |
|---|---|---|
| [`backend-java-onion`](../modules/backend-java-onion/README.md) | Java/Spring-Boot-Grundgerüst in Onion-Architektur: Ringe, JSpecify-Nullness (NullAway), jMolecules-Stereotypen, ArchUnit-Regeln, die vier Testebenen (`test`/`adapterTest`/`apiTest`/`archTest`) als Gradle-Tasks | — (Fundament) |
| [`frontend-react-vite`](../modules/frontend-react-vite/README.md) | React/Vite-Frontend, Build ins Backend-Jar eingebettet, Vitest-Testebene | `backend-java-onion` |
| [`persistence-postgres-flyway`](../modules/persistence-postgres-flyway/README.md) | Postgres über Flyway-Migrationen, JdbcTemplate-Repository-Muster (kein Spring Data), Testcontainers-Pflicht, optionale Aktivierung über eine Property | `backend-java-onion` |
| [`deployment-fly-cloudflare`](../modules/deployment-fly-cloudflare/README.md) | Fly.io-Hosting (eine Instanz), Dockerfile, GitHub-Actions-CI/CD, Semantic Release, optionale Cloudflare-Vorlage (DNS/Proxy) | — (unabhängig, aber sinnvoll erst mit einem fertig bauenden Projekt) |
| [`quality-gates`](../modules/quality-gates/README.md) | Mutationstests (Pitest) auf kritischen Klassen, ein JGiven-Report über alle Testebenen | `backend-java-onion` (hängt Gates an dessen Test-Tasks) |
| [`dependabot-maintenance`](../modules/dependabot-maintenance/README.md) | Dependabot-Konfiguration, CodeQL, OpenRewrite für Major-Sprünge, die tägliche Routine, die offene Dependabot-PRs sichtet und mergt | `backend-java-onion` (praktisch), optional `frontend-react-vite`/`quality-gates`/`deployment-fly-cloudflare` |

## Empfohlene Reihenfolge beim Einspielen

1. **`backend-java-onion`** immer zuerst — alle anderen Module hängen sich
   in dessen Ringe/Tasks ein.
2. **`quality-gates`** direkt danach, bevor Fachcode entsteht — ein Gate,
   das erst nachträglich eingespielt wird, hat schon unbewertete Commits
   durchgelassen.
3. **`frontend-react-vite`** und **`persistence-postgres-flyway`**
   unabhängig voneinander, je nach Projektbedarf (auch keins von beiden,
   z. B. für ein reines API-Backend ohne eigene DB).
4. **`deployment-fly-cloudflare`** zuletzt, wenn ein Projekt lokal baut und
   testet — Deployment vor einem laufenden Build zu verdrahten bringt
   nichts.
5. **`dependabot-maintenance`** ganz am Ende — die Routine setzt einen
   funktionierenden, gebauten Build voraus, gegen den sie später Merges
   prüfen kann.

## Was dieser Katalog nicht ist

Kein Modul enthält Fachcode oder ein Prozessmodell (Feature-Doku-Ablauf,
ADR-Kette, Entscheidungsprotokoll o. ä.) — das Blueprint-Repo liefert
ausschließlich die technischen Bausteine (Struktur, Gates, Deployment).
Wie ein konkretes Projekt von der Idee bis zum Commit arbeitet, ist Sache
des Projekts selbst.
