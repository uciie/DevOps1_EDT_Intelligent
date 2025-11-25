import { BrowserRouter, Routes, Route, Link, Navigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import Home from "./pages/Home";
import About from "./pages/About";
import RegisterPage from "./pages/RegisterPage";
import LoginPage from "./pages/LoginPage";
import SchedulePage from "./pages/SchedulePage";
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
  };

  return (
    <DndProvider backend={HTML5Backend}>
      <BrowserRouter>
        <div className="app">
          <nav className="app-nav">
            <div className="nav-brand">
              <Link to="/">📅 EDT Intelligent</Link>
            </div>

            <div className="nav-links">
              <Link to="/" className="nav-link">Accueil</Link>
              {currentUser && (
                <Link to="/schedule" className="nav-link">Mon Emploi du Temps</Link>
              )}
              <Link to="/about" className="nav-link">À propos</Link>
            </div>

            <div className="nav-actions">
              {currentUser ? (
                <>
                  <span className="user-badge">
                    👤 {currentUser.username}
                  </span>
                  <button onClick={handleLogout} className="btn-logout">
                    Déconnexion
                  </button>
                </>
              ) : (
                <>
                  <Link to="/login" className="btn-nav-login">
                    Se connecter
                  </Link>
                  <Link to="/register" className="btn-nav-register">
                    S"inscrire
                  </Link>
                </>
              )}
            </div>
          </nav>

          <main className="app-main">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<LoginPage />} />
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
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </DndProvider>
  );
}

export default App;