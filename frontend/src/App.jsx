import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import Home from "./pages/Home";
import About from "./pages/About";
import RegisterPage from "./pages/RegisterPage";

function App() {
  useEffect(() => {
    fetch(`${import.meta.env.VITE_API_URL}/users`)
      .then((r) => r.json())
      .then((data) => console.log("Utilisateurs :", data))
      .catch((err) => console.error("Erreur API:", err));
  }, []);

  return (
    <div>
      <h1>EDT intelligent 😎</h1>
      <BrowserRouter>
        <nav
          style={{
            display: "flex",
            gap: "1rem",
            marginBottom: "1rem",
            background: "#f5f5f5",
            padding: "0.5rem 1rem",
            borderRadius: "8px",
          }}
        >
          <Link to="/">Accueil</Link>
          <Link to="/about">À propos</Link>
          <Link to="/register">S'inscrire</Link>
        </nav>

        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/about" element={<About />} />
          <Route path="/register" element={<RegisterPage />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
