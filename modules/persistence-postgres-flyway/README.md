# persistence-postgres-flyway

Liefert das Muster für Postgres-Persistenz ohne Spring Data: Flyway-Migrationen
für das Schema, `NamedParameterJdbcTemplate` für die Repository-Adapter,
manuelles DataSource/Flyway-Wiring statt Spring-Boot-Autoconfiguration, und
eine Testcontainers-Basis für alle DB-Tests.

## Abhängigkeit

Setzt `backend-java-onion` voraus: dieses Modul liefert die Port/Adapter-Ringe
(`application/port/out`, `adapter/out/*`), in die sich die hier erzeugten
Dateien einhängen. Ohne dieses Fundament gibt es keine Stelle, an der
`ExampleRepositoryJdbc` als Adapter für einen Port stehen könnte.

- Port (Interface) liegt in `application/port/out/` — z. B. `ExampleRepository`.
  Dieses Modul erzeugt keinen eigenen Port, weil dessen Form vom jeweiligen
  Fachmodell abhängt; `ExampleRepositoryJdbc` im Template zeigt aber, wie ein
  Adapter dazu aussieht, und ein Port-Interface nach demselben Muster
  (`save`, `findById`, `delete`, …) ist beim Einspielen mit anzulegen.
- Adapter (Implementierung) landet in `adapter/out/db/`.
- Konfiguration (DataSource + Flyway-Wiring) landet in `config/` (bzw. einem
  Unterpaket, falls das Zielprojekt seine Konfiguration nach Fachbereich
  aufteilt, z. B. `config/{{DB_SCHEMA_LABEL}}`).

## Platzhalter

| Platzhalter | Bedeutung | Beispiel |
|---|---|---|
| `{{PACKAGE_BASE}}` | Basis-Package des Zielprojekts | `de.fourteen.example` |
| `{{app}}` | Präfix für Spring-Properties | `example` (→ `example.league.db.url`) |
| `{{DB_SCHEMA_LABEL}}` | Name des Migrations-Unterverzeichnisses unter `db/` | `league` (→ `classpath:db/league/migration`) |
| `{{ExampleEntity}}` / `{{ExampleId}}` | Platzhalter-Fachtyp, beim Einspielen durch das echte Domänenmodell zu ersetzen | — |

Die Property, die die Aktivierung steuert, heißt nach diesem Schema
`{{app}}.db.url` (Beispielwert: `example.league.db.url`).

## Optionale Aktivierung

Das zentrale Muster: `@ConditionalOnProperty(prefix
= "{{app}}.db", name = "url")` auf der Konfigurationsklasse. Fehlt die
Property, entsteht kein einziger Bean dieser Klasse — die Kernanwendung
startet trotzdem, der Datenbank-Teil bleibt einfach funktionslos. Das ist die
Voreinstellung für lokale Entwicklung und für Tests, die keine Datenbank
brauchen. Diese Bedingtheit ist die Grundlage dafür, dass ein
Datenbank-Modul überhaupt optional zuschaltbar sein kann — sie gehört daher
mit auf jede Bean, die von der DataSource abhängt (Repository-Beans
eingeschlossen), nicht nur auf die DataSource selbst.

Spring Boots eigene DataSource-/Flyway-Autoconfiguration ist dabei in der
Hauptanwendungsklasse des Zielprojekts abzuschalten (über `exclude` in
`@SpringBootApplication`) — sonst konkurriert sie mit dem
manuellen Wiring hier. Dieses Modul erzeugt diese Ausschluss-Zeile nicht
selbst, weil sie in der Hauptklasse des Zielprojekts steht, die dieses Modul
nicht besitzt; der Generator-Skill muss sie beim Einspielen ergänzen.

## Warum kein Spring Data

Siehe `docs/muster-db-adapter.md`.

## Dateien dieses Moduls

- `build.gradle.fragment.kts` — Dependency-Zeilen zum Einspielen in
  `build.gradle.kts` des Zielprojekts.
- `template/DatabaseConfig.java.template` — DataSource (Hikari) + Flyway,
  bedingt aktiviert.
- `template/ExampleRepositoryJdbc.java.template` — Repository-Adapter-Muster
  (Upsert über `NamedParameterJdbcTemplate`).
- `template/migration/V1__example_table.sql` — Beispielmigration.
- `template/AbstractPostgresIntegrationTest.java.template` — Testcontainers-
  Basisklasse (Singleton-Container).
- `docs/muster-db-adapter.md` — Begründung des Musters.

## Offene Anmerkungen

- Ob der Generator-Skill `{{DB_SCHEMA_LABEL}}` immer setzt oder auch ein
  Projekt ohne Fachbereichs-Unterverzeichnis unterstützen soll (Migrationen
  direkt unter `classpath:db/migration`), ist hier bewusst offengelassen —
  beide Varianten sind mit dem Muster vereinbar, aber nur eine ist im
  Template ausformuliert.
- Nutzername/Passwort können als eigene Properties existieren oder
  vollständig in der JDBC-URL stehen (`ExampleDatabaseConfig` lässt dafür
  leere Strings als Default zu) — dieses Modul trifft hier bewusst keine
  Vorgabe.
- Dieses Modul nimmt keine Aussage zu Connection-Pool-Feineinstellungen
  (Pool-Größe, Timeouts) vor und belässt es bei den Hikari-Defaults; das
  liegt außerhalb des Scopes dieses Moduls, weil sinnvolle Werte stark vom
  Lastprofil des jeweiligen Projekts abhängen.
