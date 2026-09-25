# Muster: DB-Adapter über JdbcTemplate + Flyway

## Warum JdbcTemplate statt Spring Data

Spring Data JPA bringt einen eigenen Persistenzmechanismus (Entity-Manager,
Lazy Loading, Session-Zustand) mit, der der Onion-Architektur (ADR-024)
entgegensteht: Domänentypen sollen kein Framework kennen und keine
versteckten Ladezustände tragen. `NamedParameterJdbcTemplate` bleibt dagegen
reines SQL plus Parameterbindung — der Adapter bildet Domänentyp und
Datenbankzeile von Hand aufeinander ab (`RowMapper`), ohne dass die Domäne
davon etwas mitbekommt. Das macht jede Abbildung explizit lesbar (was in der
Tabelle steht und was das Aggregat trägt, muss nicht übereinstimmen) und
verhindert, dass ein Framework unbeobachtet SQL nachlädt.

## Warum manuelles Wiring statt Autoconfiguration

Spring Boots eigene DataSource- und Flyway-Autoconfiguration erzeugt Beans,
sobald die passenden Abhängigkeiten und Properties vorhanden sind — ohne
eine Möglichkeit, das an eine eigene Bedingung zu knüpfen. Das Muster hier
braucht aber genau das: eine Datenbank, die vollständig fehlen darf, ohne
dass die Anwendung deswegen nicht startet (siehe README.md, Abschnitt
„Optionale Aktivierung"). Deshalb schaltet das Zielprojekt die
Autoconfiguration ab und verdrahtet DataSource, Flyway-Migration und
`NamedParameterJdbcTemplate` in einer eigenen `@Configuration`-Klasse von
Hand, mit `@ConditionalOnProperty` auf einer einzigen Property als
Schalter — derselbe Stil eignet sich für jede weitere, ebenfalls optional
zuschaltbare, von Hand verdrahtete Konfigurationsklasse des Zielprojekts.

## Warum Testcontainers Pflicht ist

Ein Repository-Adapter, der nur gegen eine H2- oder sonstige
In-Memory-Datenbank getestet wird, prüft nicht dieselbe SQL-Dialekt- und
Constraint-Semantik, die in Produktion läuft (z. B. `ON CONFLICT` ist
Postgres-spezifisch). Ein echter, in Tests gestarteter Postgres-Container
schließt diese Lücke, ohne dass jemand von Hand eine Datenbank auf der
CI-Maschine bereitstellen muss. Deshalb gilt projektweit: Datenbanken in
Tests kommen ausschließlich aus Testcontainers, auf jeder Ebene, auch in
E2E — kein selbst abgesetztes `docker run` in einem Start- oder CI-Skript.

Damit das nicht das Zeitbudget einer Testphase sprengt (ein Container-Start
kostet spürbar Zeit), startet `AbstractPostgresIntegrationTest` genau einen
Container für den gesamten Testlauf (Singleton-Pattern über ein
`static { ... }`-Blockinit), migriert ihn einmal und leert die Tabellen vor
jedem einzelnen Test — statt für jede Testklasse oder gar jede Testmethode
neu zu starten.

## Offene Anmerkung

Wie ein Zielprojekt mit mehreren, voneinander unabhängigen
Datenbank-nutzenden Fachbereichen umgehen soll (mehrere
`{{DB_SCHEMA_LABEL}}`-Migrationsverzeichnisse, mehrere Container in
Tests?), ist bewusst offengelassen — das Muster ist bislang nur für den
Ein-Fachbereich-Fall geprüft und liegt für den Mehr-Fachbereich-Fall
außerhalb des aktuellen Scopes dieses Moduls.
