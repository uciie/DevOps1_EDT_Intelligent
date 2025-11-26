import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { registerUser } from "../api/userApi";
import "../styles/pages/RegisterPage.css";

function RegisterPage() {
  const [formData, setFormData] = useState({ username: "", password: "" });
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    // Validation du mot de passe
    if (formData.password !== confirmPassword) {
      setError("Les mots de passe ne correspondent pas");
      return;
    }

    if (formData.password.length < 4) {
      setError("Le mot de passe doit contenir au moins 4 caractères");
      return;
    }

    setLoading(true);

    try {
      // Appel API centralisé
      const userId = await registerUser(formData);

      // Stockage de l'utilisateur
      const user = { id: userId, username: formData.username };
      localStorage.setItem("currentUser", JSON.stringify(user));

      // Redirection vers l'emploi du temps
      navigate("/schedule", { replace: true });
    } catch (err) {
      console.error("Erreur lors de l'inscription:", err);
      setError("Ce nom d'utilisateur est déjà pris ou une erreur est survenue");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-page">
      <div className="register-container">
        <div className="register-header">
          <h1>Créer un compte</h1>
          <p>Rejoignez-nous et organisez votre temps efficacement</p>
        </div>

        <form className="register-form" onSubmit={handleSubmit}>
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
              placeholder="Choisissez un nom d'utilisateur"
              required
              autoFocus
              minLength={3}
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
              minLength={4}
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirmer le mot de passe</label>
            <input
              type="password"
              id="confirmPassword"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirmez votre mot de passe"
              required
              minLength={4}
            />
          </div>

          <button type="submit" className="btn-register" disabled={loading}>
            {loading ? (
              <>
                <span className="spinner"></span>
                Création en cours...
              </>
            ) : (
              "Créer mon compte"
            )}
          </button>
        </form>

        <div className="register-footer">
          <p>
            Vous avez déjà un compte ?{" "}
            <Link to="/login" className="link-login">
              Se connecter
            </Link>
          </p>
        </div>
      </div>

      <div className="register-illustration">
        <div className="illustration-content">
          <h2>EDT Intelligent</h2>
          <p>Rejoignez des milliers d'utilisateurs</p>
          <ul className="benefits-list">
            <li>✅ Gratuit et sans engagement</li>
            <li>🔒 Vos données sont sécurisées</li>
            <li>⚡ Configuration rapide</li>
            <li>📱 Accessible partout</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;