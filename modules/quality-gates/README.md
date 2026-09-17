# quality-gates

Liefert das, was den Unterschied zwischen einer Konvention und einem Gate
macht: Mutationstests (Pitest) auf gezielt ausgewählten, kritischen
Klassen, und ein einziger JGiven-Report über alle Test-Ebenen hinweg.

Kernaussage dieses Moduls:

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
| `docs/gates-uebersicht.md` | Tabelle: welches Gate prüft was und greift immer. |

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

Die Pitest-Zielklassen werden über eine eigene `@Criticality(HIGH)`-Annotation
ausgewählt (Reflection über die kompilierten Klassen, keine Textsuche,
keine zweite handgepflegte Liste, die still veraltet). Die konkreten
Kritikalitätsstufen (LOW/MEDIUM/HIGH) und Klassennamen sind dabei
Projektentscheidungen, nur das Prinzip ist generalisiert:

- `build.gradle.fragment.kts` erwartet eine Annotation
  `{{PACKAGE_BASE}}.criticality.Critical` (Platzhalter, siehe unten) mit
  einer `level()`-Methode, und sammelt darüber alle Klassen im
  konfigurierten Pitest-Level (Gradle-Property `pitestLevel`, Default
  `HIGH`) per Reflection aus den kompilierten `main`-Klassen ein.
- Eine leere Zielmenge bricht den Build ab (`GradleException`) statt still
  einen wirkungslosen Pitest-Lauf zuzulassen — ein wirkungsloser Lauf wäre
  ein grünes Gate, das nichts prüft, und damit schlimmer als gar kein
  Gate.
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

Jeder der Test-Tasks aus `backend-java-onion` (`test`, `adapterTest`, `apiTest`) bekommt denselben
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
  Feature-Dokument-Pflichtformat** — diese Muster sind sinnvolle,
  ergänzende Gates, liegen aber außerhalb des Scopes dieses Moduls: sie
  hängen an projektspezifischen Formaten (Commit-Konventionen,
  Ausnahmenregister-Format, Protokoll-/Feature-Dokumentstruktur), die
  dieses Modul nicht vorgibt. Wer sie braucht, richtet sich einen eigenen
  Hook/Task nach demselben Prinzip ein: ein Skript oder ein Gradle-Task,
  der an `check` hängt und den Build hart abbricht, wenn die Konvention
  verletzt ist.

## Offene Anmerkungen

- `abdeckung` (Feature-Abdeckung gegen ein Anforderungsregister) und
  `abdeckungFrontend` (dieselbe Messung fürs Frontend) sind sinnvolle,
  nicht überspringbare Gates, hängen aber an einem bestimmten
  Anforderungsdokument-Format (`docs/anforderungen.md`, Anhang A) bzw. an
  `frontend-react-vite`. `abdeckungFrontend` ist laut Aufgabenstellung
  Bestandteil von `frontend-react-vite`; `abdeckung` (Backend-Fassung) ist
  keinem der drei Module hier eindeutig zugeordnet — dieses Modul
  übernimmt es nicht, um keine Überschneidung zu riskieren. Das ist eine
  bewusste Lücke, keine übersehene.
- Die Ebenen-Disjunktheit (kein Adapter-/API-Test darf eine Domänenzeile
  abdecken, die kein unit-/port-Test selbst erreicht) hängt an den
  JaCoCo-Reports der Onion-Test-Ebenen aus `backend-java-onion` und gehört
  inhaltlich eher dorthin. Hier nur als Querverweis erwähnt, nicht
  implementiert.
