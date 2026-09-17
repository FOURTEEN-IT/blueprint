/**
 * Der Kern. Kein Spring, kein Jackson, kein WebSocket/HTTP.
 *
 * Traegt den Onion-Ring {@code @DomainModelRing} (jMolecules) UND liegt im
 * Paket {@code domain.model} — {@code ArchitectureTest.ringeTragenIhreAnnotation}
 * prueft, dass beides zusammenpasst. Jeder oeffentliche Typ hier braucht
 * ausserdem genau einen DDD-Baustein-Stereotyp ({@code @AggregateRoot},
 * {@code @Entity} oder {@code @ValueObject}), siehe
 * {@code ArchitectureTest.jederDomaenentypTraegtEinenBaustein} — eine
 * Ausnahme (z. B. ein reiner Katalog ohne Identitaet oder Wert) gehoert
 * explizit in die Ausnahmeliste dort, nicht stillschweigend hierher.
 */
@org.jmolecules.architecture.onion.classical.DomainModelRing
@org.jspecify.annotations.NullMarked
package {{PACKAGE_BASE}}.domain.model;
