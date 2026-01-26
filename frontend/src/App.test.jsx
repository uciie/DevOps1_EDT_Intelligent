import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi} from "vitest";
import App from "./App";

// Empêche le fetch d'échouer
globalThis.fetch = vi.fn(() =>
  Promise.resolve({
    json: () => Promise.resolve([]),
  })
);

// Ce test vérifie que ton titre principal s'affiche bien
describe("App component", () => {
  it("renders the main heading", () => {
    render(<App />);
    const heading = screen.getByRole("heading", { name: /bienvenue sur edt intelligent/i });
    expect(heading).toBeInTheDocument();
  });
});
