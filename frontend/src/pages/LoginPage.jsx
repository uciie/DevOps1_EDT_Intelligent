import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { loginUser } from "../api/authApi";
import "../styles/pages/LoginPage.css";

function LoginPage({ onLogin }) {
  const [formData, setFormData] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError(""); // Réinitialiser l'erreur lors de la modification
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      // Appel API de connexion
      const user = await loginUser(formData);

      // Stockage de l'utilisateur connecté
      localStorage.setItem("currentUser", JSON.stringify(user));

      // CORRECTION ICI : On informe App.jsx que l'utilisateur est connecté
      // Cela permet de mettre à jour la navbar instantanément sans recharger la page
      if (onLogin) {
        onLogin(user);
      }

      // Redirection vers l'emploi du temps
      navigate("/schedule", { replace: true });
    } catch (err) {
      console.error("Erreur de connexion:", err);
      setError("Nom d'utilisateur ou mot de passe incorrect");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-header">
          <h1>Connexion</h1>
          <p>Accédez à votre emploi du temps</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          {error && (
            <div className="error-message">
              <span>⚠️</span> {error}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="username">Nom d'utilisateur</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Entrez votre nom d'utilisateur"
              required
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Mot de passe</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Entrez votre mot de passe"
              required
            />
          </div>

          <button type="submit" className="btn-login" disabled={loading}>
            {loading ? (
              <>
                <span className="spinner"></span>
                Connexion en cours...
              </>
            ) : (
              "Se connecter"
            )}
          </button>
        </form>

        <div className="login-footer">
          <p>
            Pas encore de compte ?{" "}
            <Link to="/register" className="link-register">
              Créer un compte
            </Link>
          </p>
        </div>
      </div>

      <div className="login-illustration">
        <div className="illustration-content">
          <h2>EDT Intelligent</h2>
          <p>Organisez votre temps efficacement</p>
          <ul className="features-list">
            <li>📅 Calendrier intuitif</li>
            <li>✅ Gestion des tâches</li>
            <li>🎯 Priorisation automatique</li>
            <li>⏰ Rappels intelligents</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;