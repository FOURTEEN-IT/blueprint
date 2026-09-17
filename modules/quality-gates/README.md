# quality-gates

Liefert das, was aus der Referenzimplementierung heraus den
Unterschied zwischen einer Konvention und einem Gate macht: Mutationstests
(Pitest) auf gezielt ausgewählten, kritischen Klassen, und ein einziger
JGiven-Report über alle Test-Ebenen hinweg.

Kernaussage, wörtlich aus der Referenzimplementierung übernommen (`CLAUDE.md`):

> Ein übersprungener Skill ist kein Beinbruch, ein übersprungenes Gate gibt
> es nicht.

Skills (Reihenfolge, Vorgehen, Urteilssache) lassen sich überspringen. Ein
Gate, das an `check` hängt, lässt sich nicht überspringen — unabhängig
davon, ob jemand einen Skill aufgerufen hat, und unabhängig davon, ob ein
Mensch oder ein Agent committiert. Genau das liefert dieses Modul: nicht
die Test-Ebenen selbst (die kommen aus `backend-java-onion` bzw.
`frontend-react-vite`), sondern die Verdrahtung, die aus ihrer bloßen
Existenz eine erzwungene Eigenschaft des Builds macht.

## Was hier drinsteckt

| Datei/Ordner | Zweck |
|---|---|
| `build.gradle.fragment.kts` | Pitest-Plugin + Konfiguration (Zielklassen über eine Kritikalitäts-Annotation, siehe unten), JGiven-Report-Wiring über mehrere Test-Tasks. Beides hängt explizit an `check`. |
| `docs/gates-uebersicht.md` | Tabelle: welches Gate prüft was, greift immer, Referenz auf die Referenzimplementierung als Beispiel. |

## Abhängigkeit zu anderen Modulen

- **`backend-java-onion` (Voraussetzung):** Dieses Modul hängt Pitest und
  JGiven-Reporting an Test-**Tasks**, die es selbst nicht definiert —
  `test`/`adapterTest`/`apiTest`/`archTest` kommen aus `backend-java-onion`.
  Ohne dieses Modul referenziert das Fragment hier nicht existierende Tasks
  und lässt sich nicht einspielen.
- **`frontend-react-vite`:** Definiert `npmTest`/`abdeckungFrontend` selbst;
  dieses Modul überschneidet sich damit nicht und wiederholt sie nicht.
  Erwähnt in `docs/gates-uebersicht.md` nur als Querverweis.

## Mutationstests: Zielklassenauswahl

Die Referenzimplementierung leitet die Pitest-Zielklassen aus einer selbst
geschriebenen `@Criticality(HIGH)`-Annotation ab (Reflection über die
kompilierten Klassen, keine Textsuche, keine zweite handgepflegte Liste,
die still veraltet). Dieses Modul generalisiert das Prinzip, ohne die
dortigen Kritikalitätsstufen (LOW/MEDIUM/HIGH) oder Klassennamen zu
übernehmen:

- `build.gradle.fragment.kts` erwartet eine Annotation
  `{{PACKAGE_BASE}}.criticality.Critical` (Platzhalter, siehe unten) mit
  einer `level()`-Methode, und sammelt darüber alle Klassen im
  konfigurierten Pitest-Level (Gradle-Property `pitestLevel`, Default
  `HIGH`) per Reflection aus den kompilierten `main`-Klassen ein.
- Eine leere Zielmenge bricht den Build ab (`GradleException`) statt still
  einen wirkungslosen Pitest-Lauf zuzulassen — derselbe Fehlermodus, den
  die Referenzimplementierung an dieser Stelle ausdrücklich vermeidet
  (Kommentar im Referenz-`build.gradle.kts`, Abschnitt „Mutationstests auf
  den HIGH-Klassen").
- `includedGroups` filtert auf die Tags `unit`/`port` (aus
  `backend-java-onion`) — kein Spring, kein Socket, kein Reportschreiben
  während der Mutation, sonst wird der Lauf unbenutzbar.

**Offene Anmerkung:** Die konkrete Zahl der Kritikalitätsstufen (Beispielwert:
LOW/MEDIUM/HIGH) und der Schwellwert (Beispielwert: 99 % mutationThreshold)
sind Projektentscheidungen, keine generalisierbare Vorgabe dieses Moduls —
`build.gradle.fragment.kts` markiert beides als Platzhalter
(`{{PITEST_MUTATION_THRESHOLD}}`), setzt aber keinen Default vor, der wie
eine Empfehlung aussehen könnte, ohne am echten Projekt kalibriert zu sein.

## JGiven-Report über alle Ebenen

Wie in der Referenzimplementierung: Jeder der aus `backend-java-onion` übernommenen
Test-Tasks (`test`, `adapterTest`, `apiTest`) bekommt denselben
`resultsDir` für sein JGiven-`JGivenTaskExtension`, und ein einziger
`jgivenTestReport`-Task liest von dort — ein Report mit allen Szenarien
aus allen Ebenen, statt drei getrennten. `archTest` bleibt bewusst außen
vor: ArchUnit-Regeln sind keine JGiven-Szenarien und schreiben nie in
diesen Ordner (eine Abhängigkeit darauf wäre ein Validierungsfehler ohne
Gegenwert — Gradle bemängelt sonst eine „implicit dependency" auf ein
Verzeichnis, das der Task nie befüllt).

## Was ein Nutzer anpassen muss

Platzhalter (`{{...}}`):

- `{{PACKAGE_BASE}}` — Basispaket für die Kritikalitäts-Annotation.
- `{{PITEST_MUTATION_THRESHOLD}}` — Mindest-Mutation-Score in Prozent, am
  echten Projekt kalibriert, keine Vorgabe dieses Moduls.

## Nicht Teil dieses Moduls

- **`commit-msg`-Hook, Ausnahmenregister, Protokollvertrag/Vertragstest,
  Feature-Dokument-Pflichtformat** — diese Muster existieren in der
  Referenzimplementierung, sind hier aber bewusst nicht aufgenommen (an
  anderer Stelle bereits vorhanden). Wer sie braucht, orientiert sich am
  Original in der Referenzimplementierung
  (`.githooks/commit-msg`, `ci/commit-format-pruefen.sh`,
  `ausnahmenregister`-Task, `protokollvertrag`/`protokollvertragLiga`,
  `featuredoku`).

## Offene Anmerkungen

- `abdeckung` (Feature-Abdeckung gegen ein Anforderungsregister) und
  `abdeckungFrontend` (dieselbe Messung fürs Frontend) sind in der
  Referenzimplementierung ebenfalls nicht überspringbare Gates, hängen aber an einem bestimmten
  Anforderungsdokument-Format (`docs/anforderungen.md`, Anhang A) bzw. an
  `frontend-react-vite`. `abdeckungFrontend` ist laut Aufgabenstellung
  Bestandteil von `frontend-react-vite`; `abdeckung` (Backend-Fassung) ist
  keinem der drei Module hier eindeutig zugeordnet — dieses Modul
  übernimmt es nicht, um keine Überschneidung zu riskieren. Das ist eine
  bewusste Lücke, keine übersehene.
- Die Ebenen-Disjunktheit (`ebenenDisjunktheit` in der Referenzimplementierung: kein
  Adapter-/API-Test darf eine Domänenzeile abdecken, die kein
  unit-/port-Test selbst erreicht) hängt an den JaCoCo-Reports der
  Onion-Test-Ebenen aus `backend-java-onion` und gehört inhaltlich eher
  dorthin. Hier nur als Querverweis erwähnt, nicht implementiert.
