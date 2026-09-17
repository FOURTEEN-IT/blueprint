# ADR-026: JSpecify-Nullness mit NullAway durchgesetzt

**Status:** Vorlage — im Zielprojekt mit eigener Nummer übernehmen.

**Referenzimplementierung:** Watchparty-Projekt, ADR-026.

**Kontext:** Ohne eine im Typsystem sichtbare Nullness sehen ein Feld, das
garantiert gesetzt ist, und ein Feld, das fehlen darf, im Code identisch
aus — nur Disziplin und Kommentare unterscheiden sie. Das öffnet genau die
Lücke, die zu einer `NullPointerException` weit entfernt von der
eigentlichen Ursache führt.

**Entscheidung:** JSpecify-Annotationen (`org.jspecify:jspecify`) markieren
Nullability im Typsystem, NullAway (über Error Prone) erzwingt sie beim
Kompilieren als Fehler, nicht als Warnung.

- **`@NullMarked` auf jedem Ring** (`domain`, `application`, `adapter`,
  `config`, je eine `package-info.java`). Jeder Verweistyp ist dort
  nicht-null, sofern nicht ausdrücklich `@Nullable`.
- **NullAway läuft im `OnlyNullMarked`- und `JSpecifyMode`-Modus:** geprüft
  wird ausschließlich markierter Code, alles andere (Spring, Jackson, die
  JDK selbst) bleibt „legacy" und wird nicht mitgeprüft.
- **Testcode ist bewusst nicht `@NullMarked`.** Tests bauen häufig Objekte
  in unvollständigen Zwischenzuständen; das soll nicht dieselbe Disziplin
  tragen wie das Modell selbst. `compileTestJava` läuft deshalb ganz ohne
  Error Prone.
- **Nur NullAway läuft**, keine der übrigen Error-Prone-Prüfungen
  (`disableAllChecks` plus die getypte NullAway-Plugin-DSL). Diese
  Einrichtung soll Null-Sicherheit durchsetzen, keinen Stilkatalog.
- **`@Nullable` steht direkt vor dem betroffenen Typ**, auch bei einem
  qualifizierten Typ (`Foo.@Nullable Bar`, nicht `@Nullable Foo.Bar`).
- **Wo eine Nicht-Null-Bedingung nicht aus dem Typ folgt, aber aus der
  Struktur** (ein Map-Zugriff, dessen Schlüssel nachweislich existiert),
  macht ein `Objects.requireNonNull(...)` mit Begründungskommentar die
  Annahme sichtbar, statt sie stillschweigend vorauszusetzen.

**Konsequenzen:**
- Der Compiler ist die Durchsetzung, nicht eine Konvention — ein
  `@Nullable` an der falschen Stelle im Domänenmodell ist ein Build-Fehler.
- NullAways lokale Dataflow-Analyse verlangt an einigen Stellen einen
  direkten Null-Check auf dieselbe Variable, selbst wenn die Nichtigkeit
  logisch schon aus einem vorherigen Aufruf folgt — ein zusätzlicher,
  redundant wirkender Check ist dann kein Bug, sondern notwendig, damit der
  Compiler dieselbe Garantie sieht wie der Mensch.
