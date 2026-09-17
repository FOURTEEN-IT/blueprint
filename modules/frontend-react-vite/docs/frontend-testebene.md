# Frontend-Testebene

Eine eigene Testebene fuer sichtbares Frontend-Verhalten, getrennt von
Unit-/Integrationstests im Backend und von einer moeglichen E2E-Ebene
(echter Browser, langsamer, meist nicht Teil von `check`).

## Werkzeug

Vitest + Testing Library, Umgebung `jsdom`. Kein echter Browser -- was einen
echten Browser braucht (Layout, Timing, Mobile-Eigenheiten), gehoert auf eine
separate E2E-Ebene, nicht hierher.

## Grundsatz

**Serverdaten rein, sichtbare Ausgabe raus.** Ein Test dieser Ebene speist
eine Nachricht oder einen Zustand ein, wie ihn das Backend liefern wuerde,
und prueft, was auf dem Bildschirm erscheint. Fachliche Regeln (Berechnung,
Validierung, Zustandsuebergaenge) werden hier nicht ein zweites Mal geprueft
-- die sind Sache des Backends bzw. seiner eigenen Testebenen. Diese Ebene
prueft die Projektion: Kommt aus einem gegebenen Serverzustand die richtige
Darstellung heraus.

## Anforderungs-Tag

`tests/requirement.js` stellt `requirement(ids, name, fn)` bereit: ein
duenner Wrapper um `it(...)`, der die Anforderungs-ID(s) dem Testnamen
voranstellt und sie maschinell auffindbar macht. Ein Abdeckungs-Gate (siehe
`build.gradle.fragment.kts`, Abschnitt "Abdeckungs-Gate") kann diese Aufrufe
gegen einen eigenen Anforderungskatalog abgleichen -- das Konzept ist
generalisiert, das Gate selbst ist projektspezifisch und deshalb nicht Teil
dieses Moduls.

## Beispielimplementierung

Watchparty (`frontend/tests/`) zeigt eine ausgebaute Fassung: ein
`setup.js` mit projektspezifischen Grundannahmen je Test, eine geteilte
Zustandsform (`zustand.js`) fuer wiederkehrende Serverdaten in Tests, sowie
den Abdeckungs-Task `abdeckungFrontend` in `build.gradle.kts`, der Anhang A
eines Anforderungsdokuments gegen `anforderung(...)`-Aufrufe (dort so
benannt) abgleicht.
