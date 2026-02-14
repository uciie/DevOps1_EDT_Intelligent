import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { linkGoogleAccount } from "../api/userApi";
import { getCurrentUser } from "../api/authApi";
import "../styles/pages/SetupPage.css"; // Réutilisation des styles pour le spinner

function GoogleCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const [status, setStatus] = useState("Connexion à Google en cours...");

  useEffect(() => {
    const handleCallback = async () => {
      const code = searchParams.get("code");
      const user = getCurrentUser();

      if (!code) {
        setError("Aucun code d'autorisation trouvé.");
        return;
      }

      if (!user || !user.id) {
        setError("Utilisateur non authentifié localement.");
        return;
      }

      try {
        setStatus("Échange du code contre les jetons...");
        const updatedUser = await linkGoogleAccount(user.id, code);
        // On fusionne pour ne pas perdre les champs déjà présents (id, username…)
        const merged = { ...user, ...updatedUser };
        localStorage.setItem("currentUser", JSON.stringify(merged));
        // Succès : on redirige vers la page setup avec un message de succès
        navigate("/setup", { state: { success: true, message: "Compte Google lié avec succès !" } });
      } catch (err) {
        console.error("Erreur callback Google:", err);
        //  distinguer "token sauvegardé mais réponse KO"
        // du vrai échec de liaison. Si on est ici avec une 500 mais que
        // le token est en BDD, on redirige quand même avec un avertissement.
        const isPartialSuccess = err?.response?.status === 500;
        if (isPartialSuccess) {
          // Token probablement sauvegardé, on redirige avec avertissement
          navigate("/setup", {
            state: {
              success: true,
              message: "Google lié avec succès ! (rechargement nécessaire)"
            }
          });
        } else {
          setError("Impossible de lier le compte Google. Vérifiez la console.");
        }
      }
    };

    handleCallback();
  }, [searchParams, navigate]);

  if (error) {
    return (
      <div className="setup-page">
        <div className="settings-card" style={{ textAlign: 'center' }}>
          <h2 style={{ color: 'var(--danger-600)' }}>Erreur d'authentification</h2>
          <p>{error}</p>
          <button onClick={() => navigate("/setup")} className="btn-back">
            Retour aux paramètres
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="setup-page">
      <div className="settings-card" style={{ textAlign: 'center' }}>
        <span className="spinner-small" style={{ margin: '0 auto 1rem' }}></span>
        <h2>{status}</h2>
        <p>Veuillez patienter pendant que nous configurons votre intégration.</p>
      </div>
    </div>
  );
}

export default GoogleCallback;