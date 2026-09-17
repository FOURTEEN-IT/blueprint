import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeEach } from "vitest";

// Jeder Test beginnt auf einem leeren Geraet: kein localStorage-Rest, keine
// URL von einem vorherigen Test. Sonst haengt das Ergebnis davon ab, welcher
// Test vorher lief. Projektspezifische Grundannahmen (z. B. "Anleitung schon
// gesehen") gehoeren hier ergaenzt, nicht in einzelne Tests verstreut.
beforeEach(() => {
  window.localStorage.clear();
  window.history.replaceState(null, "", "/");
});

afterEach(() => {
  cleanup();
});
