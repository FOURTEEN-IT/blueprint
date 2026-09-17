/**
 * Der Anforderungs-Tag der Frontend-Ebene -- das Gegenstueck zu einer
 * Anforderungs-Annotation im Backend, falls das eigene Projekt sowas fuehrt.
 *
 * Er tut zweierlei: Er stellt die Anforderungs-ID(s) dem Leser des Testlaufs
 * voran, und er macht sie im Quelltext maschinell auffindbar -- ein
 * Abdeckungs-Task kann diese Aufrufe einlesen und mit dem eigenen
 * Anforderungskatalog abgleichen (siehe docs/frontend-testebene.md). Deshalb
 * muessen die IDs hier als Zeichenketten stehen und duerfen nicht erst zur
 * Laufzeit entstehen.
 *
 * In build.gradle.fragment.kts heisst der zugehoerige Gate-Task
 * `abdeckungFrontend`; er erwartet genau dieses Aufrufmuster.
 */
import { it } from "vitest";

export function requirement(ids, name, fn) {
  const list = Array.isArray(ids) ? ids : [ids];
  return it(`${list.join(", ")}: ${name}`, fn);
}
