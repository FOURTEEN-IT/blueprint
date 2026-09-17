/**
 * Domain Services: reine Funktionen, die zu keiner einzelnen Entity
 * gehoeren (Referenzbeispiel: {@code Settlement} im Watchparty-Projekt).
 * {@code @Service}-Typen sind zustandslos —
 * {@code ArchitectureTest.domainServicesSindZustandslos} prueft das.
 */
@org.jmolecules.architecture.onion.classical.DomainServiceRing
@org.jspecify.annotations.NullMarked
package {{PACKAGE_BASE}}.domain.service;
