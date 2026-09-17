# quality-gates

Liefert das, was aus der Referenzimplementierung (Watchparty) heraus den
Unterschied zwischen einer Konvention und einem Gate macht: Mutationstests
(Pitest) auf gezielt ausgewählten, kritischen Klassen, ein einziger
JGiven-Report über alle Test-Ebenen hinweg, der `commit-msg`-Hook gegen
Conventional Commits samt Release-Plausibilitätsprüfung, und optional das
Ausnahmenregister-Muster für bewusst unterdrückte Prüfungen.

Kernaussage, wörtlich aus Watchparty übernommen (`CLAUDE.md`):

> Ein übersprungener Skill ist kein Beinbruch, ein übersprungenes Gate gibt
> es nicht.

Skills (Reihenfolge, Vorgehen, Urteilssache) lassen sich überspringen. Ein
Gate, das an `check` hängt, oder ein Git-Hook, der vor dem Commit greift,
lässt sich nicht überspringen — unabhängig davon, ob jemand einen Skill
aufgerufen hat, und unabhängig davon, ob ein Mensch oder ein Agent
committiert. Genau das liefert dieses Modul: nicht die Test-Ebenen selbst
(die kommen aus `backend-java-onion` bzw. `frontend-react-vite`), sondern
die Verdrahtung, die aus ihrer bloßen Existenz eine erzwungene Eigenschaft
des Builds macht.

## Was hier drinsteckt

| Datei/Ordner | Zweck |
|---|---|
| `build.gradle.fragment.kts` | Pitest-Plugin + Konfiguration (Zielklassen über eine Kritikalitäts-Annotation, siehe unten), JGiven-Report-Wiring über mehrere Test-Tasks, optional das Ausnahmenregister-Task-Muster. Alles hängt explizit an `check`. |
| `.githooks-template/commit-msg` | Generalisierter Git-Hook: ruft `ci-template/commit-format-pruefen.sh` mit der Commit-Nachrichtendatei auf. |
| `ci-template/commit-format-pruefen.sh` | Generalisierte Fassung des Conventional-Commits-Checks aus Watchparty: erlaubte Typen, und die Zusatzregel „ein releasender Typ ohne jede Anwendungsdatei ist vermutlich falsch getippt". Läuft sowohl als `commit-msg`-Hook (eine Nachrichtendatei als Argument) als auch gegen eine Commit-Reihe in CI (`<basis-ref> <kopf-ref>`). |
| `docs/gates-uebersicht.md` | Tabelle: welches Gate prüft was, greift immer, Referenz auf Watchparty als Beispiel. |

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

Watchparty leitet die Pitest-Zielklassen aus einer selbst geschriebenen
`@Criticality(HIGH)`-Annotation ab (Reflection über die kompilierten
Klassen, keine Textsuche, keine zweite handgepflegte Liste, die still
veraltet). Dieses Modul generalisiert das Prinzip, ohne Watchparjs eigene
Kritikalitätsstufen (LOW/MEDIUM/HIGH) oder Klassennamen zu übernehmen:

- `build.gradle.fragment.kts` erwartet eine Annotation
  `{{PACKAGE_BASE}}.criticality.Critical` (Platzhalter, siehe unten) mit
  einer `level()`-Methode, und sammelt darüber alle Klassen im
  konfigurierten Pitest-Level (Gradle-Property `pitestLevel`, Default
  `HIGH`) per Reflection aus den kompilierten `main`-Klassen ein.
- Eine leere Zielmenge bricht den Build ab (`GradleException`) statt still
  einen wirkungslosen Pitest-Lauf zuzulassen — derselbe Fehlermodus, den
  Watchparty an dieser Stelle ausdrücklich vermeidet (Kommentar im
  Referenz-`build.gradle.kts`, Abschnitt „Mutationstests auf den
  HIGH-Klassen").
- `includedGroups` filtert auf die Tags `unit`/`port` (aus
  `backend-java-onion`) — kein Spring, kein Socket, kein Reportschreiben
  während der Mutation, sonst wird der Lauf unbenutzbar.

**Offene Anmerkung:** Die konkrete Zahl der Kritikalitätsstufen (Watchparty:
LOW/MEDIUM/HIGH) und der Schwellwert (Watchparty: 99 % mutationThreshold)
sind Projektentscheidungen, keine generalisierbare Vorgabe dieses Moduls —
`build.gradle.fragment.kts` markiert beides als Platzhalter
(`{{PITEST_MUTATION_THRESHOLD}}`), setzt aber keinen Default vor, der wie
eine Empfehlung aussehen könnte, ohne am echten Projekt kalibriert zu sein.

## JGiven-Report über alle Ebenen

Wie in Watchparty: Jeder der aus `backend-java-onion` übernommenen
Test-Tasks (`test`, `adapterTest`, `apiTest`) bekommt denselben
`resultsDir` für sein JGiven-`JGivenTaskExtension`, und ein einziger
`jgivenTestReport`-Task liest von dort — ein Report mit allen Szenarien
aus allen Ebenen, statt drei getrennten. `archTest` bleibt bewusst außen
vor: ArchUnit-Regeln sind keine JGiven-Szenarien und schreiben nie in
diesen Ordner (eine Abhängigkeit darauf wäre ein Validierungsfehler ohne
Gegenwert — Gradle bemängelt sonst eine „implicit dependency" auf ein
Verzeichnis, das der Task nie befüllt).

## commit-msg-Hook

`.githooks-template/commit-msg` und `ci-template/commit-format-pruefen.sh`
sind Watchparty eins zu eins nachgebaut, nur ohne watchparty-spezifische
Strings:

- Erlaubte Commit-Typen: das Angular-Preset, das
  `@semantic-release/commit-analyzer` per Default versteht
  (`build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test`) — keine
  eigene Konvention, sondern die Menge, die das Release-Werkzeug tatsächlich
  auslöst.
- Zusatzregel: Ein releasender Typ (`feat`/`fix`/`perf`) ohne jede
  geänderte Anwendungsdatei ist vermutlich falsch getippt oder falsch
  gewählt — das Muster aus Watchparty (`ci/commit-format-pruefen.sh`,
  Prozess-Audit vom 2026-08-21), verallgemeinert über den Platzhalter
  `{{ANWENDUNGSPFADE}}` (Watchparty:
  `src/main/|frontend/src/|frontend/package(-lock)?\.json$|build\.gradle\.kts$|settings\.gradle\.kts$|Dockerfile$|fly\.toml$`)
  statt der dort fest verdrahteten Pfade.
- Zwei Aufrufmodi wie im Original: `commit-msg`-Hook (eine Nachrichtendatei
  als einziges Argument) und CI-Modus gegen eine Commit-Reihe
  (`<basis-ref> <kopf-ref>`, ohne Argumente nur `HEAD`).
- Aktivierung je Klon über `git config core.hooksPath .githooks` (wie in
  Watchparty) — `.git/hooks` liegt außerhalb der Versionskontrolle, deshalb
  dieser Umweg über ein versioniertes Verzeichnis.

## Was ein Nutzer anpassen muss

Platzhalter (`{{...}}`):

- `{{PACKAGE_BASE}}` — Basispaket für die Kritikalitäts-Annotation.
- `{{PITEST_MUTATION_THRESHOLD}}` — Mindest-Mutation-Score in Prozent, am
  echten Projekt kalibriert, keine Vorgabe dieses Moduls.
- `{{ANWENDUNGSPFADE}}` — Regex der Pfade, deren Änderung ein Release
  rechtfertigt (siehe oben).

## Optionale, generalisierte Konzepte

Die folgenden zwei Watchparty-Gates sind fachlich zu eng verwoben, um sie
unverändert zu übernehmen (sie lesen ein bestimmtes Anforderungsdokument-
Format bzw. ein bestimmtes WebSocket-Protokoll). Ihr *Prinzip* ist aber
generalisierbar; wer es will, baut sich daraus ein eigenes, projektspezifisches
Gate — dieses Modul liefert dafür nur die Beschreibung und eine Vorlage,
keinen fertigen Task.

### Ausnahmenregister (aus Watchparty: `ausnahmenregister`, `docs/test-ausnahmen.md`)

Prinzip: Jede bewusste Unterdrückung einer Prüfung im Code (Watchparty:
`@AequivalenterMutant` gegen einen Pitest-Mutanten, `@Disabled` gegen einen
JUnit-Test) muss in einem versionierten Register mit Begründung und Datum
stehen — und umgekehrt darf das Register keine Karteileichen enthalten
(eine Unterdrückung, die im Code nicht mehr existiert). Ein Gate gleicht
beide Seiten per Reflection ab und bricht in beide Richtungen: Unterdrückung
ohne Eintrag, oder Eintrag ohne Unterdrückung.

Das ist direkt generalisierbar, weil es an keine Fachlichkeit gebunden ist —
`build.gradle.fragment.kts` enthält den Task `ausnahmenregister` deshalb
tatsächlich als Task (kein reines Konzept), mit denselben zwei Annotationen
(`AequivalenterMutant` unter `{{PACKAGE_BASE}}.mutationtest`, `@Disabled`)
und einer Registerdatei unter `docs/test-ausnahmen.md`.

### Protokollvertrag / Vertragstest (aus Watchparty: `protokollvertrag`, `protokollvertragLiga`)

Prinzip: Ein Vertragstest, der beweist, dass ein zwischen zwei getrennt
gebauten Seiten vereinbartes Nachrichtenschema (bei Watchparty: WebSocket-
Frame-Typen und Feldnamen zwischen Backend und Frontend) und dessen
Dokumentation nicht auseinanderlaufen — ohne dass eine der beiden Seiten
die Änderung der anderen automatisch sieht. Watchparty prüft das über
einen Abgleich von Literalen im Backend-Quelltext gegen Literale im
Frontend-Quelltext.

Dieses Prinzip ist in Watchparty eng an das konkrete WebSocket-Protokoll
und dessen Feldnamen gebunden; eine unmittelbare Generalisierung ohne ein
konkretes zweites Beispielprojekt wäre geraten statt begründet. Dieses
Modul übernimmt deshalb **keinen** fertigen Task dafür, sondern hält das
Prinzip hier fest, als Anregung für ein Projekt mit einer vergleichbaren
Frontend/Backend-Grenze.

### Feature-Dokument-Pflichtformat (aus Watchparty: `featuredoku`)

Prinzip: Ein Gate, das ein Pflichtformat für Feature-Dokumente erzwingt
(Pflichtabschnitte, genau eine Kritikalitätsstufe, eine Höchstzahl an
Akzeptanzkriterien als Schnitt-Disziplin, eine Pflichttabelle gegen ein
Anforderungsregister). Wie beim Protokollvertrag ist die konkrete
Ausprägung (Watchparty: Anhang-A-IDs, die Abschnittsnamen selbst) so
projektspezifisch, dass dieses Modul sie nicht kopiert — das Prinzip
„ein Feature-Dokument als geprüftes, nicht nur ermahntes Format" gehört
eher zum Projekt-Prozess-Skill-Layer (`schneiden`/`feature` in Watchparty)
als zu einem generischen Gradle-Gate. Nicht Teil dieses Moduls.

## Offene Anmerkungen

- `abdeckung` (Feature-Abdeckung gegen ein Anforderungsregister) und
  `abdeckungFrontend` (dieselbe Messung fürs Frontend) sind in Watchparty
  ebenfalls nicht überspringbare Gates, hängen aber an einem bestimmten
  Anforderungsdokument-Format (`docs/anforderungen.md`, Anhang A) bzw. an
  `frontend-react-vite`. `abdeckungFrontend` ist laut Aufgabenstellung
  Bestandteil von `frontend-react-vite`; `abdeckung` (Backend-Fassung) ist
  keinem der drei Module hier eindeutig zugeordnet — dieses Modul
  übernimmt es nicht, um keine Überschneidung zu riskieren. Das ist eine
  bewusste Lücke, keine übersehene.
- Die Ebenen-Disjunktheit (`ebenenDisjunktheit` in Watchparty: kein
  Adapter-/API-Test darf eine Domänenzeile abdecken, die kein
  unit-/port-Test selbst erreicht) hängt an den JaCoCo-Reports der
  Onion-Test-Ebenen aus `backend-java-onion` und gehört inhaltlich eher
  dorthin. Hier nur als Querverweis erwähnt, nicht implementiert.
