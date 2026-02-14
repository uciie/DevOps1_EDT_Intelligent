import { useState, useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import { getCurrentUser } from "../api/authApi";
import { recalculateTravelTimes } from "../api/eventApi";
import { getGoogleAuthUrl, linkGoogleAccount, unlinkGoogleAccount} from "../api/userApi";
import api from "../api/api";
import Notification from "../components/Notification";
import "../styles/pages/SetupPage.css";

function SetupPage() {
  const location = useLocation(); // Pour intercepter le state du navigate
  const [useGoogleMaps, setUseGoogleMaps] = useState(true);
  const [status, setStatus] = useState(""); // Message d'état
  const [loading, setLoading] = useState(false); // Pour bloquer l'UI pendant le recalcul
  const [user, setUser] = useState(null);
  const [isGoogleConnected, setIsGoogleConnected] = useState(false);
  const [notification, setNotification] = useState({ show: false, message: "", type: "" });
  const [isUnlinking, setIsUnlinking] = useState(false);

  const handleGoogleLogin = () => {
      window.location.href = getGoogleAuthUrl();
  };

  useEffect(() => {
    const fetchData = async () => {
      const currentUser = getCurrentUser();
      if (currentUser) {
        setUser(currentUser);

        try {
          // 1. On récupère les infos fraîches du backend pour voir si les tokens sont là
          const response = await api.get(`/users/username/${currentUser.username}`);
          const userData = response.data;
          setUser(userData);
          
          // Vérifie si le champ googleRefreshToken est présent (signe de connexion)
          if (userData.googleRefreshToken) {
            setIsGoogleConnected(true);
          }
        } catch (err) {
          console.error("Erreur lors de la récupération de l'utilisateur", err);
        }
      }

      // 2. On vérifie si le composant GoogleCallback nous a envoyé un message de succès
      if (location.state?.success) {
        setNotification({
          show: true,
          message: location.state.message || "Connexion Google réussie !",
          type: "success"
        });
        // Nettoie l'URL pour ne pas réafficher la notification au prochain refresh
        window.history.replaceState({}, document.title);
      }
    };

    fetchData();
    
    const savedPref = localStorage.getItem("useGoogleMaps");
    if (savedPref !== null) {
      try {
        setUseGoogleMaps(JSON.parse(savedPref));
      } catch (e) {
        console.warn('Invalid useGoogleMaps value in localStorage, ignoring.');
      }
    }
  }, [location]);

  // Fonction pour fermer la notification
  const closeNotification = () => setNotification({ ...notification, show: false });

  const handleToggle = async (e) => {
    const isChecked = e.target.checked;
    
    // 1. Mise à jour visuelle immédiate du switch
    setUseGoogleMaps(isChecked);
    localStorage.setItem("useGoogleMaps", JSON.stringify(isChecked));

    if (!user) return;

    // 2. Déclenchement du recalcul côté serveur
    try {
      setLoading(true);
      setStatus("Recalcul des trajets en cours... Veuillez patienter.");
      
      await recalculateTravelTimes(user.id, isChecked);
      
      setStatus("Mise à jour terminée ! Rechargement...");
      
      // 3. Rafraîchissement complet du site pour recharger les données (Events, TravelTimes)
      // On attend une petite seconde pour que l'utilisateur voie le message de succès
      setTimeout(() => {
        window.location.href = "/schedule";
      }, 1000);

    } catch (error) {
      console.error(error);
      setStatus("Erreur lors de la mise à jour des trajets.");
      setLoading(false);
    }
  };

  // Fonction pour se déconnecter de Google
  const handleGoogleUnlink = async () => {
    if (!user) return;
    if (!window.confirm("Voulez-vous vraiment déconnecter votre compte Google Agenda ?")) return;

    try {
      setIsUnlinking(true);
      setStatus("Déconnexion de Google en cours...");

      await unlinkGoogleAccount(user.id);

      setIsGoogleConnected(false);
      setNotification({
        show: true,
        message: "Compte Google déconnecté avec succès.",
        type: "success"
      });
      setStatus("");

    } catch (error) {
      console.error("Erreur déconnexion Google:", error);
      setNotification({
        show: true,
        message: "Erreur lors de la déconnexion. Réessayez.",
        type: "error"
      });
    } finally {
      setIsUnlinking(false);
    }
  };

  return (
    <div className="setup-page">
      {/* Affichage du popup Notification */}
      {notification.show && (
        <Notification 
          message={notification.message} 
          type={notification.type} 
          onClose={closeNotification} 
        />
      )}
      <div className="setup-container">
        <div className="setup-header">
          <h1>⚙️ Configuration</h1>
          <p>Personnalisez le comportement de l'application</p>
        </div>

        <div className="settings-card">
          <div className="setting-item">
            <div className="setting-info">
              <h3>Mode de calcul des trajets</h3>
              <p>
                Activez pour utiliser l'API Google Maps (plus précis, trafic temps réel).
                Désactivez pour utiliser un calcul simple (vol d'oiseau, plus rapide).
              </p>
              <p className="warning-text">
                ⚠️ Changer ce paramètre recalculera tous vos trajets existants.
              </p>
            </div>
            <div className="setting-control">
              <label className="switch">
                <input 
                  type="checkbox" 
                  checked={useGoogleMaps} 
                  onChange={handleToggle} 
                  disabled={loading} // Désactive le switch pendant le chargement
                />
                <span className="slider round"></span>
              </label>
            </div>
          </div>
          {/* --- Authentification Google --- */}
          <hr className="setup-divider" /> {/* Optionnel : pour séparer visuellement */}
          
          <section className="integrations-section">
            <h3>Intégrations</h3>
            <div className="setting-item"> {/* Utilisation de 'setting-item' pour garder le même style CSS */}
              <div className="setting-info">
                <span>Google Calendar : <strong>{isGoogleConnected ? "Connecté" : "Déconnecté"}</strong></span>
              </div>
              <div className="setting-control">
                <button 
                  onClick={handleGoogleLogin} 
                  className={`btn-google ${isGoogleConnected ? 'connected' : ''}`}
                  disabled={isUnlinking}
                >
                  {!isGoogleConnected && (
                    <svg className="google-icon" viewBox="0 0 24 24">
                      <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                      <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                      <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                      <path d="M12 5.38c1.62 0 3.06.56 4.21 1.66l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 12-4.53z" fill="#EA4335"/>
                    </svg>
                  )}
                  {isGoogleConnected ? "Reconnecter Google" : "Se connecter avec Google"}
                </button>

                {/* Bouton déconnexion — visible seulement si connecté */}
                {isGoogleConnected && (
                  <button
                    onClick={handleGoogleUnlink}
                    disabled={isUnlinking}
                    className="btn-google-unlink"
                    title="Révoquer l'accès Google Agenda"
                  >
                    {isUnlinking ? "Déconnexion..." : "Déconnecter"}
                  </button>
                )}
              </div>
            </div>
          </section>
          {/* --- Fin Authentification Google --- */}

          {/* Zone de notification / chargement */}
          <div className={`status-message ${loading ? 'loading' : ''}`}>
             {loading && <span className="spinner-small"></span>}
             <span>{status}</span>
          </div>
        </div>

        <div className="setup-actions">
          <Link to="/schedule" className={`btn-back ${loading ? 'disabled' : ''}`}>
            ← Retour à l'emploi du temps
          </Link>
        </div>
      </div>
    </div>
  );
}

export default SetupPage;