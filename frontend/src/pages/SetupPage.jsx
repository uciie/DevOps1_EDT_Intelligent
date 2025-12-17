import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { getCurrentUser } from "../api/authApi";
import { recalculateTravelTimes } from "../api/eventApi";
import "../styles/pages/SetupPage.css";

function SetupPage() {
  const [useGoogleMaps, setUseGoogleMaps] = useState(true);
  const [status, setStatus] = useState(""); // Message d'état
  const [loading, setLoading] = useState(false); // Pour bloquer l'UI pendant le recalcul
  const [user, setUser] = useState(null);

  useEffect(() => {
    // Récupérer l'utilisateur courant
    const currentUser = getCurrentUser();
    setUser(currentUser);

    // Récupérer la préférence locale
    const savedPref = localStorage.getItem("useGoogleMaps");
    if (savedPref !== null) {
      setUseGoogleMaps(JSON.parse(savedPref));
    }
  }, []);

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

  return (
    <div className="setup-page">
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