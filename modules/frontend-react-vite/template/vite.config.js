import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  // Frontend-Testebene (siehe docs/frontend-testebene.md): Serverdaten rein,
  // sichtbare Ausgabe raus. jsdom statt echtem Browser -- was einen echten
  // Browser braucht, gehoert auf eine E2E-Ebene, nicht hierher.
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./tests/setup.js"],
    include: ["tests/**/*.test.{js,jsx}"],
    reporters: ["default", ["json", { outputFile: "../build/reports/frontend-tests.json" }]],
  },
  server: {
    // Dev-Proxy: Vite bedient nur die React-App auf :5173, Backend-Anfragen
    // gehen unveraendert an den Spring-Boot-Prozess auf {{BACKEND_DEV_PORT}}.
    // Pfade an das eigene Projekt anpassen -- "/ws" ist nur ein Beispiel fuer
    // einen WebSocket-Endpunkt, "/api" fuer einen REST-Endpunkt. Projekte
    // ohne WebSocket lassen den ws-Block einfach weg.
    proxy: {
      "/ws": {
        target: "ws://localhost:{{BACKEND_DEV_PORT}}",
        ws: true,
      },
      "/api": {
        target: "http://localhost:{{BACKEND_DEV_PORT}}",
      },
    },
  },
});
