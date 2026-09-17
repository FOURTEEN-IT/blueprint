# ADR-024: Onion-Architektur mit Ringen, Ports und Adaptern

**Status:** Vorlage — im Zielprojekt mit eigener Nummer und ggf. angepasster
Begründung übernehmen.

**Kontext:** Ohne eine erzwungene Abhängigkeitsrichtung wandert Framework-
Code (Spring, ein HTTP-Client, ein Nachrichten-Objekt mit Jackson-
Annotationen) unbemerkt in den fachlichen Kern. Ein Kern, der Spring kennt,
lässt sich nicht mehr ohne Spring-Kontext testen oder instanziieren, und
jede Abhängigkeit nach außen ist ein potenzieller Fachlogik-Leck nach
außen — z. B. wenn ein Domänentyp anfängt, Persistenz-Interna zu kennen.

**Entscheidung:** Der Code wird in Ringe geschnitten, Abhängigkeiten zeigen
ausschließlich nach innen:

`domain.model` ← `domain.service` ← `application` (inklusive `port.in` und
`port.out`) ← `adapter.*` (in/out). `config` ist die Kompositionswurzel
außerhalb der Ringe und die einzige Stelle mit Framework-Beans.

- **Ports statt konkreter Infrastruktur.** Alles, was der Anwendungsring
  gegenüber der Außenwelt braucht (Persistenz, Uhrzeit, Versand), ist ein
  Interface in `application.port.out`; die konkrete Implementierung liegt
  im Adapter-Ring.
- **Domänentypen kennen keine Adapter.** Ein Aggregat, das sein eigenes
  Speicherformat kennen müsste, bricht die Regel — stattdessen entsteht ein
  eigenes, vom Modell unabhängiges Format (z. B. ein `*Snapshot`-Typ,
  bewusst kein Modellbaustein).
- **Die Ringregel ist ein Test, keine Konvention.** `ArchitectureTest`
  prüft auf Bytecode-Ebene (Rückgabetypen, Feldtypen, nicht nur
  Importzeilen).

**Konsequenzen:**
- Mehr Pakete und mehr Typen für dieselbe Fachlichkeit — bei Backends
  jenseits eines Wochenend-Prototyps vertretbar, weil der Kern dadurch
  wirklich framework-frei und ohne Spring-Kontext instanziierbar bleibt.
- Ein neuer Adapter-Ring (z. B. eine zweite Persistenz-Technologie) braucht
  eine eigene Zeile in `onionArchitecture().adapter(name, package)` —
  ArchUnit meldet sonst entweder eine leere Architektur oder lässt den
  neuen Adapter unbeaufsichtigt durch.
- `config` selbst wird bewusst nicht geprüft: Es ist die einzige Stelle, an
  der alles zusammenlaufen darf.
