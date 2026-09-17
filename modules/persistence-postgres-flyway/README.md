# persistence-postgres-flyway

Liefert das Muster für Postgres-Persistenz ohne Spring Data: Flyway-Migrationen
für das Schema, `NamedParameterJdbcTemplate` für die Repository-Adapter,
manuelles DataSource/Flyway-Wiring statt Spring-Boot-Autoconfiguration, und
eine Testcontainers-Basis für alle DB-Tests. Referenzimplementierung ist das
Tippspiel-Modul des Ursprungsprojekts (`adapter/out/db`, `config/league/ExampleDatabaseConfig`,
`src/main/resources/db/league/migration`).

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
  aufteilt, wie es die Referenzimplementierung mit `config/league` tut).

## Platzhalter

| Platzhalter | Bedeutung | Beispiel aus der Referenzimplementierung |
|---|---|---|
| `{{PACKAGE_BASE}}` | Basis-Package des Zielprojekts | `de.fourteen.example` |
| `{{app}}` | Präfix für Spring-Properties | `example` (→ `example.league.db.url`) |
| `{{DB_SCHEMA_LABEL}}` | Name des Migrations-Unterverzeichnisses unter `db/` | `league` (→ `classpath:db/league/migration`) |
| `{{ExampleEntity}}` / `{{ExampleId}}` | Platzhalter-Fachtyp, beim Einspielen durch das echte Domänenmodell zu ersetzen | — |

Die Property, die die Aktivierung steuert, heißt nach diesem Schema
`{{app}}.db.url` (Beispielwert: `example.league.db.url`).

## Optionale Aktivierung

Das zentrale Muster aus `LeagueDatabaseConfig`: `@ConditionalOnProperty(prefix
= "{{app}}.db", name = "url")` auf der Konfigurationsklasse. Fehlt die
Property, entsteht kein einziger Bean dieser Klasse — die Kernanwendung
startet trotzdem, der Datenbank-Teil bleibt einfach funktionslos. Das ist die
Voreinstellung für lokale Entwicklung und für Tests, die keine Datenbank
brauchen. Diese Bedingtheit ist die Grundlage dafür, dass ein
Datenbank-Modul überhaupt optional zuschaltbar sein kann — sie gehört daher
mit auf jede Bean, die von der DataSource abhängt (Repository-Beans
eingeschlossen), nicht nur auf die DataSource selbst.

Spring Boots eigene DataSource-/Flyway-Autoconfiguration ist dabei in der
Hauptanwendungsklasse des Zielprojekts abzuschalten (in der
Referenzimplementierung über `exclude` in `@SpringBootApplication`) — sonst konkurriert sie mit dem
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
  direkt unter `classpath:db/migration`), ist hier nicht entschieden — beide
  Varianten kommen in der Referenzimplementierung nicht nebeneinander vor
  (dort gibt es nur `db/league/migration`, weil es bislang nur einen
  Datenbank-nutzenden Fachbereich gibt).
- Ob Nutzername/Passwort immer als eigene Properties existieren oder auch
  vollständig in der JDBC-URL stehen dürfen (wie es `ExampleDatabaseConfig`
  mit leeren Strings als Default zulässt), ist bewusst so übernommen wie im
  Original — keine eigene Erweiterung.
- Dieses Modul nimmt keine Aussage zu Connection-Pool-Feineinstellungen
  (Pool-Größe, Timeouts) vor, weil die Referenzimplementierung selbst keine
  über die Hikari-Defaults hinaus setzt.
