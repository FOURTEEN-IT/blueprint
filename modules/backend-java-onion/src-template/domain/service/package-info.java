/**
 * Domain Services: reine Funktionen, die zu keiner einzelnen Entity
 * gehoeren (z. B. eine Berechnung, die mehrere Aggregate einbezieht).
 * {@code @Service}-Typen sind zustandslos —
 * {@code ArchitectureTest.domainServicesSindZustandslos} prueft das.
 */
@org.jmolecules.architecture.onion.classical.DomainServiceRing
@org.jspecify.annotations.NullMarked
package {{PACKAGE_BASE}}.domain.service;
