/**
 * Domain Services: reine Funktionen, die zu keiner einzelnen Entity
 * gehoeren (Referenzbeispiel: {@code Settlement} in der Referenzimplementierung).
 * {@code @Service}-Typen sind zustandslos —
 * {@code ArchitectureTest.domainServicesSindZustandslos} prueft das.
 */
@org.jmolecules.architecture.onion.classical.DomainServiceRing
@org.jspecify.annotations.NullMarked
package {{PACKAGE_BASE}}.domain.service;
