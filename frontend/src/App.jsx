import { BrowserRouter, Routes, Route, Link, Navigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import Home from "./pages/Home";
import About from "./pages/About";
import RegisterPage from "./pages/RegisterPage";
import LoginPage from "./pages/LoginPage";
import SchedulePage from "./pages/SchedulePage";
import ActivityPage from "./pages/ActivityPage";
import { getCurrentUser, logoutUser } from "./api/authApi";
import PrivateRoute from "./components/PrivateRoute";
import "./styles/App.css";

function App() {
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    const user = getCurrentUser();
    setCurrentUser(user);
  }, []);

  const handleLogout = () => {
    logoutUser();
    setCurrentUser(null);
    // Redirection optionnelle vers l'accueil après déconnexion
    window.location.href = '/'; 
  };

  return (
    // pour le drag-and-drop
    <DndProvider backend={HTML5Backend}>
      {/* BrowserRouter pour le routage */}
      <BrowserRouter>
        <div className="app">
          <nav className="app-nav">
            <div className="nav-brand">
              <Link to="/">📅 EDT Intelligent</Link>
            </div>

            <div className="nav-links">
              <Link to="/" className="nav-link">Accueil</Link>
              {currentUser && (
                <>
                <Link to="/schedule" className="nav-link">Mon Emploi du Temps</Link>
                <Link to="/activity" className="nav-link">Activités</Link> 
                </>
              )}
              <Link to="/about" className="nav-link">À propos</Link>
            </div>

            <div className="nav-actions">
              {currentUser ? (
                // CAS 1 : Utilisateur connecté
                // On affiche UNIQUEMENT le bouton de déconnexion (pas de badge pseudo)
                <button onClick={handleLogout} className="btn-logout">
                  Se déconnecter
                </button>
              ) : (
                // CAS 2 : Utilisateur non connecté
                <>
                  <Link to="/login" className="btn-nav-login">
                    Se connecter
                  </Link>
                  <Link to="/register" className="btn-nav-register">
                    S'inscrire
                  </Link>
                </>
              )}
            </div>
          </nav>

          <main className="app-main">
            <Routes>
              <Route path="/" element={<Home />} />
              {/* On passe setCurrentUser à LoginPage pour mettre à jour l'état après connexion */}
              <Route path="/login" element={<LoginPage onLogin={(user) => setCurrentUser(user)} />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/about" element={<About />} />
              <Route
                path="/schedule"
                element={
                  <PrivateRoute>
                    <SchedulePage />
                  </PrivateRoute>
                }
              />
              <Route
                path="/activity"
                element={
                  <PrivateRoute>
                    <ActivityPage />
                  </PrivateRoute>
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </DndProvider>
  );
}

export default App;