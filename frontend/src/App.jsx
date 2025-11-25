import { BrowserRouter, Routes, Route, Link, useNavigate } from "react-router-dom";
import { getCurrentUser, logoutUser } from "./api/authApi";
import Home from "./pages/Home";
import About from "./pages/About";
import RegisterPage from "./pages/RegisterPage";
import LoginPage from "./pages/LoginPage";
import SchedulePage from "./pages/SchedulePage";
import PrivateRoute from "./components/PrivateRoute";
import "./styles/App.css";

// Composant principal de l'application gérant la navigation et l'interface globale
function AppContent() {
  const navigate = useNavigate();
  const currentUser = getCurrentUser();

  const handleLogout = () => {
    logoutUser();
    navigate("/", { replace: true });
    window.location.reload(); // Forcer le rechargement pour mettre à jour l'UI
  };

  // UI principale de l'application : barre de navigation et zone de contenu
  return (
    <div className="app">
      <nav className="app-nav">
        {/* Marque / logo cliquable vers l'accueil */}
        <div className="nav-brand">
          <Link to="/">📅 EDT Intelligent</Link>
        </div>

        {/* Liens de navigation principaux */}
        <div className="nav-links">
          <Link to="/" className="nav-link">
            Accueil
          </Link>

          {/* Lien vers l'emploi du temps uniquement si l'utilisateur est connecté */}
          {currentUser && (
            <Link to="/schedule" className="nav-link">
              Mon Emploi du Temps
            </Link>
          )}

          <Link to="/about" className="nav-link">
            À propos
          </Link>
        </div>

        {/* Actions utilisateur : affichage du badge + déconnexion ou connexions/inscription */}
        <div className="nav-actions">
          {currentUser ? (
            <>
              {/* Affiche le nom d'utilisateur quand connecté */}
              <span className="user-badge">{currentUser.username}</span>

              {/* Bouton de déconnexion */}
              <button onClick={handleLogout} className="btn-logout">
                Déconnexion
              </button>
            </>
          ) : (
            <>
              {/* Liens pour se connecter / s'inscrire si non connecté */}
              <Link to="/login" className="btn-nav-login">
                Connexion
              </Link>
              <Link to="/register" className="btn-nav-register">
                S'inscrire
              </Link>
            </>
          )}
        </div>
      </nav>

      {/* Zone principale où les différentes pages sont rendues */}
      <main className="app-main">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/about" element={<About />} />

          {/* Route protégée : accessible seulement si l'utilisateur est authentifié */}
          <Route
            path="/schedule"
            element={
              <PrivateRoute>
                <SchedulePage />
              </PrivateRoute>
            }
          />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App;