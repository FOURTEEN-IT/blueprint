# backend-java-onion

Liefert das Java/Spring-Boot-Grundgerüst für ein Backend in
Onion-Architektur, so wie es die Referenzimplementierung (Watchparty)
tatsächlich umsetzt: Ringe, die nur nach innen zeigen, JSpecify-Nullness
über NullAway, jMolecules-Stereotypen für die DDD-Bausteine, ArchUnit als
geprüfte statt bloß behauptete Struktur. Kein Fachcode — das Modul liefert
das Gerüst, keine Domäne.

## Was hier drinsteckt

| Datei/Ordner | Zweck |
|---|---|
| `build.gradle.fragment.kts` | Plugin-Deklarationen, Dependencies, Testgates. Wird in das Ziel-`build.gradle.kts` eingespielt (Plugins-Block, Dependencies-Block, Task-Definitionen an den passenden Stellen zusammengeführt). |
| `src-template/` | Leeres Paketgerüst (`domain/model`, `domain/service`, `application/port/in`, `application/port/out`, `adapter/in`, `adapter/out`, `config`) plus ein fachfreies Beispiel-Trio (Aggregate Root, Entity, Value Object) mit jMolecules-Stereotyp. |
| `ArchitectureTest.java.template` | Generalisierte ArchUnit-Regeln: Ring-Richtung, Stereotyp-Zwang, Ausnahmeliste als Platzhalter. |
| `docs/adr-024-onion-architektur.md` | ADR-Vorlage: Ringstruktur. |
| `docs/adr-026-jspecify-nullness.md` | ADR-Vorlage: Nullness über NullAway. |
| `docs/adr-027-jmolecules-stereotypen.md` | ADR-Vorlage: DDD-Stereotypen + Ring-Annotationen. |

## Was ein Nutzer anpassen muss

Platzhalter in allen Fragmenten (`{{...}}`), einheitlich über das ganze
Modul:

- `{{PACKAGE_BASE}}` — Basispaket, z. B. `de.example.projectname`. Wird zu
  Verzeichnissen unter `src/main/java/` und `src/test/java/`; überall dort,
  wo Watchparty `de.fourteen.watchparty` schreibt, steht hier dieser
  Platzhalter.
- `{{PROJECT_NAME}}` — Gradle `group`/Anzeigename, in Kommentaren und im
  Feature-losen Beispielcode.
- `{{JAVA_VERSION}}` — Toolchain-Version (Watchparty: 25). Ein neues Projekt
  kann eine ältere LTS-Version wählen; die restlichen Fragmente sind davon
  unabhängig.
- `{{ADDITIONAL_ADAPTER_PORTS}}` — Zeile(n) in `onionArchitecture()...
  .adapter(...)` je nach tatsächlich vorhandenen Adaptern (`ws`, `http`,
  `file`, `db`, ...). Ohne mindestens einen Adapter-Ring meldet ArchUnit
  eine leere Architektur.
- Die Ausnahmeliste `TYPEN_OHNE_DDD_BAUSTEIN` in
  `ArchitectureTest.java.template` — anfangs leer; ein Typ kommt nur mit
  Begründung dazu (siehe ADR-027-Vorlage), nicht stillschweigend.

Nach dem Einspielen ist das Beispiel-Trio (`ExampleAggregate`,
`ExampleEntity`, `ExampleId`) ein Wegwerf-Nachweis, dass Gerüst und Tests
zusammenpassen — es wird durch den ersten echten Domänentyp ersetzt, nicht
daneben stehengelassen.

## Abhängigkeit zu anderen Modulen

- **`quality-gates` (Voraussetzung, nicht Teil dieses Moduls):** Dieses
  Modul definiert `archTest` nur als eigenen Gradle-Task und Testebene
  (Tag-Konvention `unit`/`port`/`adapter`/`api`, Engine `archunit` für
  Architekturregeln). Ob `archTest` — und `pitest`, `ArchitectureTest` als
  Gate-Name — tatsächlich an `check` hängt und den Build damit verbindlich
  bricht, entscheidet `quality-gates`. Ohne dieses Modul laufen die Tests,
  aber niemand zwingt ihre Ausführung.
- **`frontend-react-vite` (optional, additiv):** Dieses Modul bringt keinen
  Web-Ring mit eigener Meinung zu React/Vite; `adapter/in` ist bewusst leer
  vorbereitet für einen REST- oder WebSocket-Adapter, den ein Projekt
  selbst füllt. Das Frontend-Modul kommt unabhängig hinzu und baut sein
  eigenes Bundle ins Jar, ohne Rückwirkung auf die Ringe hier.
- **`persistence-postgres-flyway` (optional, additiv):** Liefert einen
  eigenen Infrastruktur-Adapter (`adapter/out/db`) samt Flyway-Migrationen.
  Dieses Modul stellt dafür nur den Ring bereit (`adapter/out` existiert,
  die Ringregel erlaubt beliebige Unterpakete darin) — es entscheidet
  nichts über JDBC vs. Spring Data, das bleibt beim Persistenz-Modul.

## Testebenen (Konvention)

Vier JUnit-Tags/Engines, disjunkt nach Reichweite, wie in der
Referenzimplementierung:

- `unit`/`port` → Task `test` (schnell, kein Spring, kein Socket).
- `adapter` → Task `adapterTest` (Adapter gegen echten Port, z. B.
  Testcontainers).
- `api` → Task `apiTest` (echter Server/Socket/HTTP-Client).
- Engine `archunit` (kein Tag — siehe Kommentar im Fragment, ArchUnit-Tags
  wurden von der JUnit-Platform-Tag-Filterung stillschweigend verschluckt)
  → eigener Task `archTest` für die Strukturprüfung.

`check` selbst bekommt diese Abhängigkeiten erst durch `quality-gates`.
