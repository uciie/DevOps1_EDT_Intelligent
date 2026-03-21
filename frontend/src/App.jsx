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
import SetupPage from "./pages/SetupPage"; 
import NotificationPage from "./pages/NotificationPage";
import GoogleCallback from "./pages/GoogleCallback";
import { getCurrentUser, logoutUser } from "./api/authApi";
import { getPendingInvitations } from "./api/teamApi";
import PrivateRoute from "./components/PrivateRoute";
import "./styles/App.css";
import FocusAiPage from "./pages/FocusAiPage"; 
import EventPage from "./components/EventPage";

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [notifCount, setNotifCount] = useState(0); 

  useEffect(() => {
    const user = getCurrentUser();
    setCurrentUser(user);
    if (user) {
      fetchNotifCount(user.id);
    }
  }, []);

  const handleLogout = () => {
    logoutUser();
    setCurrentUser(null);
    window.location.href = '/'; 
  };

  const fetchNotifCount = async (userId) => {
    try {
      const res = await getPendingInvitations(userId);
      const data = res.data || res;
      setNotifCount(data.length);
    } catch (e) {
      console.error("Erreur badge", e);
    }
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
              {/* Ajout du lien Événements accessible à tous ou seulement connectés */}
              <Link to="/events" className="nav-link">✨ Événements</Link>
              
              {currentUser && (
                <>
                  <Link to="/schedule" className="nav-link">Mon Emploi du Temps</Link>
                  <Link to="/activity" className="nav-link">Activités</Link>
                  <Link to="/focus-ai" className="nav-link">AI Document-to-Schedule </Link>
                  <Link to="/setup" className="nav-link">Configuration</Link>
                  <Link to="/notifications" className="nav-link">Notifications
                    {notifCount > 0 && <span className="notif-badge">{notifCount}</span>}
                  </Link>
                </>
              )}
              <Link to="/about" className="nav-link">À propos</Link>
            </div>

            <div className="nav-actions">
              {currentUser ? (
                <button onClick={handleLogout} className="btn-logout">
                  Se déconnecter
                </button>
              ) : (
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
              <Route path="/login" element={<LoginPage onLogin={(user) => setCurrentUser(user)} />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/about" element={<About />} />
              <Route path="/notifications" element={<NotificationPage />} />
              
              {/* --- NOUVELLE ROUTE ÉVÉNEMENTS --- */}
              <Route path="/events" element={<EventPage />} />

              {/* Routes protégées */}
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
              <Route
                path="/setup"
                element={
                  <PrivateRoute>
                    <SetupPage />
                  </PrivateRoute>
                }
              />
              <Route
                path="/focus-ai"
                element={
                  <PrivateRoute>
                    <FocusAiPage />
                  </PrivateRoute>
                }
              />
              <Route path="/google-callback" element={<GoogleCallback />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </DndProvider>
  );
}

export default App;