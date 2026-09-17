/**
 * Orchestrierung. Kennt die Domaene und ihre eigenen Ports
 * ({@code application.port.in}, {@code application.port.out}), sonst
 * nichts — insbesondere kein Spring, kein Jackson (Ausnahme nur fuer
 * Nachrichtentypen, falls das Projekt eine eigene {@code message}-
 * Unterpaket-Konvention wie die Referenzimplementierung uebernimmt).
 */
@org.jmolecules.architecture.onion.classical.ApplicationServiceRing
@org.jspecify.annotations.NullMarked
package {{PACKAGE_BASE}}.application;
