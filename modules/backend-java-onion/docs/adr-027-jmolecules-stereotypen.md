# ADR-027: jMolecules-Stereotypen für DDD-Bausteine und Onion-Ringe

**Status:** Vorlage — im Zielprojekt mit eigener Nummer übernehmen.

**Referenzimplementierung:** Ursprungsprojekt, ADR-027.

**Kontext:** „Dieser Typ ist der Aggregate Root" und „dieses Paket ist der
Domänenring" lassen sich als Satz in Javadoc und Paketname ausdrücken —
aber kein Werkzeug bemerkt, wenn ein neuer Domänentyp ohne erkennbaren
Baustein dazukommt oder eine Ringzuordnung nur zufällig über den Paketnamen
stimmt.

**Entscheidung:** jMolecules-Annotationen (`org.jmolecules:jmolecules-ddd`,
`org.jmolecules:jmolecules-onion-architecture`) markieren die Bausteine im
Code: `@AggregateRoot`, `@Entity` + `@Identity` auf dem ID-Feld,
`@ValueObject`, `@Service`. Die Onion-Ringe tragen zusätzlich
`@DomainModelRing`, `@DomainServiceRing`, `@ApplicationServiceRing`,
`@InfrastructureRing` (Variante „classical") auf den jeweiligen
`package-info.java`.

Reine Marker-Annotationen ohne Laufzeitverhalten, wie JSpecify — die
Durchsetzung übernimmt `ArchitectureTest` mit **eigenen, selbst
geschriebenen** ArchUnit-Regeln, nicht mit den vorgefertigten
`jmolecules-archunit`-Regeln: Deren zuletzt veröffentlichte Version ist
gegen eine ArchUnit-Version gebaut, die mit aktuellen ArchUnit-Ständen
`NoSuchMethodError`/`AbstractMethodError` zur Laufzeit wirft — beim
Testlauf, nicht beim Kompilieren. Ein Downgrade von ArchUnit dagegen führte
in der Referenzimplementierung dazu, dass `@AnalyzeClasses` überhaupt keine
Klassen mehr fand. Die Stereotyp-Annotationen selbst sind reine Marker und
davon unberührt — nur die Bibliothek, die sie vorgefertigt prüfen wollte,
ist es.

- `jederDomaenentypTraegtEinenBaustein`: jeder öffentliche Typ in
  `domain.model` trägt genau einen der drei DDD-Bausteine. Ausnahmen (ein
  reiner, statischer Katalog ohne Identität oder Wert) stehen explizit in
  einer Ausnahmeliste, nie stillschweigend.
- `domainServicesSindZustandslos`: `@Service`-Typen haben keine
  Instanzfelder.
- `keineOeffentlichenSetterAufEntities` /
  `keineOeffentlichenSetterAufDemAggregateRoot`: die Aggregatgrenze als
  Regel, nicht nur als paket-privater Modifier.
- `ringeTragenIhreAnnotation`: jede `package-info.java` trägt genau die
  nach der Paketstruktur vorgesehene Ring-Annotation, keine andere.

**Konsequenzen:**
- Ein Aggregate Root, von dem laut Projektregel nur genau eine Instanz
  existiert (kein Sharding, keine Mandantentrennung), trägt bewusst kein
  `@Identity` — eine Identität würde eine Unterscheidung vortäuschen, die
  es im System nicht gibt. Trifft das nicht zu (mehrere Instanzen,
  adressiert über einen Code oder eine ID), bekommt der Aggregate Root ein
  `@Identity`-Feld wie jede andere Entity.
- Ein neuer Domänentyp bekommt seinen Stereotyp sofort bei der Erstellung,
  nicht erst beim nächsten Refactoring — `ArchitectureTest` schlägt sonst
  fehl. Passt wirklich keiner, muss der Typ explizit in die Ausnahmeliste,
  nicht stillschweigend durchrutschen.
- `allowEmptyShould(true)` an beiden Setter-Regeln: „Nichts zu beanstanden"
  ist der Normalfall bei korrektem Design, nicht die Ausnahme — ohne das
  Flag würde ArchUnit eine Regel ohne Fund selbst als Fehlschlag werten.
