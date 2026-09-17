# Gates-Übersicht

Ein Gate hängt an `check` und lässt sich nicht überspringen — unabhängig
davon, ob jemand einen Prozess-Skill aufgerufen hat. Ein Skill lässt sich
überspringen, ein Gate nicht: das ist die Kernaussage dieses Moduls.

| Gate | Prüft was | Quelle | Greift immer? |
|---|---|---|---|
| `pitest` | Mutation Score auf gezielt kritischen Klassen (Reflection über eine Kritikalitäts-Annotation, keine handgepflegte Liste) | `quality-gates` | Ja — hängt an `check` |
| `jgivenTestReport` | Erzeugt den zusammengeführten JGiven-Report über `test`/`adapterTest`/`apiTest` | `quality-gates` | Ja — hängt an `check` (der Report selbst ist keine Schranke, aber das Erzeugen ist erzwungen) |
| `test`/`adapterTest`/`apiTest`/`archTest` | Die vier Onion-Test-Ebenen selbst | `backend-java-onion` | Ja — hängt an `check` |
| `ArchitectureTest`/`archTest` | Ring-Richtung, Stereotyp-Zwang | `backend-java-onion` | Ja — hängt an `check` |
| `npmTest` | Frontend-Tests (Vitest/Testing Library) | `frontend-react-vite` | Ja — hängt an `check` |
| `abdeckungFrontend` | Abgleich Frontend-Anforderungsmarken gegen grün gelaufene Testszenarien | `frontend-react-vite` | Ja — hängt an `check` |
| Ebenen-Disjunktheit | Kein Adapter-/API-Test darf eine Domänenzeile abdecken, die kein unit-/port-Test selbst erreicht | eher `backend-java-onion` (hängt an dessen JaCoCo-Reports) | Ja in der Referenzimplementierung, hier nicht implementiert — Querverweis |

`commit-msg`-Hook, Ausnahmenregister, Protokollvertrag und
Feature-Dokument-Pflichtformat sind bewusst nicht Teil dieses Moduls (siehe
README, Abschnitt „Nicht Teil dieses Moduls").

## Referenz: Ursprungsprojekt

Das Ursprungsprojekt (`CLAUDE.md`) formuliert die Kernaussage dieses Moduls wörtlich:

> Ein übersprungener Skill ist kein Beinbruch, ein übersprungenes Gate gibt
> es nicht: `featuredoku`, `abdeckung`, `abdeckungFrontend`, `npmTest`,
> `protokollvertrag`, `protokollvertragLiga`, `ArchitectureTest`, `pitest`
> und der `commit-msg`-Hook greifen unabhängig davon, ob jemand einen Skill
> aufgerufen hat.

Von dieser Liste liefert `quality-gates` `pitest` und das JGiven-Reporting.
`ArchitectureTest` und `npmTest`/`abdeckungFrontend` gehören zu
`backend-java-onion` bzw. `frontend-react-vite` — siehe die READMEs dort.

Das Ursprungsprojekt selbst hängt zusätzlich die E2E-Ebene (`e2eTest`) bewusst
**nicht** an `check`, sondern führt sie als eigene Pipeline-Stufe vor dem
Deploy — ein Browser-Durchlauf sprengt sonst das Zehn-Minuten-Budget für
`check`. Das ist kein übersprungenes Gate, sondern ein Gate an anderer
Stelle in der Pipeline.
