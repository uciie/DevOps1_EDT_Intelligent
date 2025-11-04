import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom", // Simule un navigateur
    globals: true,        // Permet d’utiliser describe, it, expect sans import
    setupFiles: "./src/setupTests.js", // Fichier de configuration avant les tests
  },
});
