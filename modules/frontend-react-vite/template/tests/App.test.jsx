import { render, screen } from "@testing-library/react";
import { describe, expect } from "vitest";
import App from "../src/App.jsx";
import { requirement } from "./requirement.js";

// Startpunkt fuer die Frontend-Testebene: ein triviales Rendertest, der
// zeigt, wie ein Szenario mit requirement() getaggt wird. Ersetzt oder
// ergaenzt werden, sobald das erste echte Feature-Modul andockt -- die ID
// hier ist ein Platzhalter, kein echter Anforderungsbezug.
describe("App", () => {
  requirement("0-a", "rendert die Huelle ohne Fehler", () => {
    render(<App />);
    expect(screen.getByRole("heading", { level: 1 })).toBeInTheDocument();
  });
});
