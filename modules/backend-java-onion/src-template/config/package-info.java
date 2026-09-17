/**
 * Kompositionswurzel: saemtliche Spring-Beans, die Adapter mit Ports
 * verdrahten. Kein eigener Ring im Onion-Sinn (ArchUnit ignoriert
 * Abhaengigkeiten von hier aus explizit), darf deshalb alles kennen.
 */
@org.jspecify.annotations.NullMarked
package {{PACKAGE_BASE}}.config;
